package org.agmas.noellesroles.roles.angel;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 天使的死亡保护处理器。
 *
 * <p>职责只有一件事：
 * 当被守护目标即将死亡时，判断是否由天使代替其赴死，并把整条回放链路补完整。</p>
 */
public final class AngelDeathProtectionHandler {

    private AngelDeathProtectionHandler() {
    }

    /**
     * 处理“天使代死守护”。
     *
     * <p>这里必须放在第一位，保持旧逻辑里的最高优先级。
     * 原实现就是要求它先于附体师护甲、潜行者免疫、Jester、试剂护盾等所有常规保命判定执行。</p>
     *
     * <p>一旦守护成立，会做三件事：</p>
     * <p>1. 给被守护目标记一条 shield_blocked 回放；</p>
     * <p>2. 把守护天使本人写进额外字段，供回放格式化器输出完整文本；</p>
     * <p>3. 立刻让天使自己以 {@code angel_sacrifice} 死因死亡。</p>
     */
    public static boolean allowDeath(PlayerEntity playerEntity, PlayerEntity killer, Identifier deathReason) {
        if (!(playerEntity instanceof ServerPlayerEntity protectedPlayer)) {
            return true;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(playerEntity.getWorld());
        for (ServerPlayerEntity possibleAngel : protectedPlayer.getServerWorld().getPlayers()) {
            if (!gameWorldComponent.isRole(possibleAngel, Noellesroles.ANGEL)) {
                continue;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(possibleAngel)) {
                continue;
            }

            AngelPlayerComponent angelComponent = AngelPlayerComponent.KEY.get(possibleAngel);
            if (angelComponent.getGuardedTarget() == null
                    || !angelComponent.getGuardedTarget().equals(protectedPlayer.getUuid())) {
                continue;
            }

            /*
             * 直接复用 wathe 的统一“挡伤来源解析”工具，
             * 这样像投掷类、无主手伤害、扩展死因等都能保持与旧实现一致的回放显示。
             */
            NbtCompound blockedReplayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
            blockedReplayData.putUuid("angel_player", possibleAngel.getUuid());
            GameRecordManager.recordShieldBlocked(
                    protectedPlayer,
                    killer instanceof ServerPlayerEntity killerPlayer ? killerPlayer : null,
                    Noellesroles.ANGEL_GUARD_SHIELD_SOURCE,
                    GameFunctions.getReplayItemId(blockedReplayData),
                    blockedReplayData
            );

            /*
             * 这个标记会被 AngelDeathCleanupMixin 读取。
             * 它的意义不是阻止本次代死，而是告诉死亡清理逻辑：
             * “这次天使死亡就是守护代死本身，不要再给被守护者发‘你的守护者死了’提示。”
             */
            angelComponent.setSacrificeDeathInProgress(true);

            NbtCompound sacrificeDeathData = new NbtCompound();
            sacrificeDeathData.putUuid("target_player", protectedPlayer.getUuid());
            GameFunctions.killPlayer(
                    possibleAngel,
                    true,
                    null,
                    Noellesroles.ANGEL_SACRIFICE_DEATH_REASON,
                    sacrificeDeathData
            );
            return false;
        }

        return true;
    }
}
