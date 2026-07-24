package org.agmas.noellesroles.roles.dreamer;

import org.agmas.noellesroles.registry.NoellesEventIds;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 梦之印记护盾死亡保护。
 *
 * <p>这个处理器只接入 NoellesRoles 的统一死亡链。
 * 强制死亡（炸弹、落轨、狙击枪等）会先由 {@code CommonForcedDeathHandler} 放行，
 * 因此梦之印记不会破坏 Noelles 现有的死亡优先级。</p>
 */
public final class DreamerDeathProtectionHandler {
    private DreamerDeathProtectionHandler() {
    }

    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        DreamerComponent dream = DreamerComponent.KEY.get(playerEntity);
        if (dream.dreamArmor <= 0) {
            return true;
        }

        if (playerEntity instanceof ServerPlayerEntity victimPlayer) {
            NbtCompound blockedReplayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
            GameRecordManager.recordShieldBlocked(
                    victimPlayer,
                    killer instanceof ServerPlayerEntity killerPlayer ? killerPlayer : null,
                    NoellesEventIds.DREAM_IMPRINT_SHIELD_SOURCE,
                    GameFunctions.getReplayItemId(blockedReplayData),
                    blockedReplayData
            );
        }

        dream.teleportToDreamer();
        dream.reset();
        return false;
    }
}
