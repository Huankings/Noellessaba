package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 灵术师的保命 / 附身善后处理器。
 *
 * <p>它承担三类职责：</p>
 * <p>1. 被附身宿主即将死亡时，由灵术师代死并保护宿主；</p>
 * <p>2. 正常解除附身后的 15 秒内，为宿主提供一次性余留庇护；</p>
 * <p>3. 如果死亡的是正在出窍/附身中的灵术师本人，先收束其特殊状态再继续死亡流程。</p>
 */
public final class SpiritualistDeathProtectionHandler {

    private SpiritualistDeathProtectionHandler() {
    }

    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        if (!(playerEntity instanceof ServerPlayerEntity victim)) {
            return true;
        }

        /*
         * 先处理“死亡者就是灵术师本人”的情况。
         *
         * 这里必须把“出窍”和“附身”分开：
         * 1. 出窍中的本体仍然允许被真正击杀，只是在死亡前先做一次回魂收束；
         * 2. 附身中的本体则必须完全免疫 killPlayer 链路，
         *    否则 wathe 的 knife / revolver 这类直接调 GameFunctions.killPlayer(...) 的击杀
         *    会绕过普通 damage 防护，把隐藏空气壳本体直接杀掉。
         */
        SpiritualistPlayerComponent selfComponent = SpiritualistPlayerComponent.KEY.get(victim);
        if (selfComponent.isPossessing()) {
            return false;
        }
        if (selfComponent.isProjecting()) {
            SpiritualistManager.cleanupDetachedState(victim);
            return true;
        }

        SpiritualistHostComponent hostComponent = SpiritualistHostComponent.KEY.get(victim);
        if (hostComponent.possessed && hostComponent.spiritualistController != null) {
            PlayerEntity controllerEntity = victim.getWorld().getPlayerByUuid(hostComponent.spiritualistController);
            if (controllerEntity instanceof ServerPlayerEntity controller
                    && GameFunctions.isPlayerAliveAndSurvival(controller)) {
                NbtCompound blockedReplayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
                blockedReplayData.putUuid("owner_player", controller.getUuid());
                GameRecordManager.recordShieldBlocked(
                        victim,
                        killer instanceof ServerPlayerEntity killerPlayer ? killerPlayer : null,
                        Noellesroles.SPIRITUALIST_ACTIVE_SHIELD_SOURCE,
                        GameFunctions.getReplayItemId(blockedReplayData),
                        blockedReplayData
                );

                SpiritualistManager.returnHostToSanctuary(victim);
                SpiritualistManager.endPossession(controller, false, false, false);

                NbtCompound deathExtra = new NbtCompound();
                deathExtra.putUuid("target_player", victim.getUuid());
                GameFunctions.killPlayer(
                        controller,
                        true,
                        null,
                        Noellesroles.SPIRITUALIST_SOUL_GUARD_DEATH_REASON,
                        deathExtra
                );
                return false;
            }
        }

        if (hostComponent.lingeringProtection
                && hostComponent.lingeringProtectionTicks > 0
                && hostComponent.lingeringOwner != null) {
            PlayerEntity lingeringOwner = victim.getWorld().getPlayerByUuid(hostComponent.lingeringOwner);
            if (lingeringOwner instanceof ServerPlayerEntity owner) {
                SpiritualistPlayerComponent ownerComponent = SpiritualistPlayerComponent.KEY.get(owner);
                if (ownerComponent.hasLingeringProtectionFor(victim)) {
                    NbtCompound blockedReplayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
                    blockedReplayData.putUuid("owner_player", owner.getUuid());
                    GameRecordManager.recordShieldBlocked(
                            victim,
                            killer instanceof ServerPlayerEntity killerPlayer ? killerPlayer : null,
                            Noellesroles.SPIRITUALIST_LINGERING_SHIELD_SOURCE,
                            GameFunctions.getReplayItemId(blockedReplayData),
                            blockedReplayData
                    );

                    SpiritualistManager.returnHostToSanctuary(victim);
                    hostComponent.consumeLingeringProtection();
                    ownerComponent.consumeLingeringProtection();
                    return false;
                }
            }
        }

        return true;
    }
}
