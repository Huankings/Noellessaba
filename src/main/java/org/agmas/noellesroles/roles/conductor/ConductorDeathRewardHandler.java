package org.agmas.noellesroles.roles.conductor;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;

/**
 * 击杀列车长时，非无辜阵营攻击者获得额外金币。
 */
public final class ConductorDeathRewardHandler {
    private static boolean initialized = false;

    private ConductorDeathRewardHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("conductor_death_reward"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                context -> {
                    /*
                     * 奖励必须在 Wathe 确认死亡后发放；死亡保护、护盾、狭缝重复请求和递归死亡
                     * 都不能触发这笔钱。攻击者还必须是当前真实存活玩家，避免旁观状态领取奖励。
                     */
                    if (!context.confirmedDeath() || context.serverKiller() == null) {
                        return;
                    }
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    if (gameWorld.isRole(context.victim(), NoellesRoleRegistry.CONDUCTOR)
                            && context.serverKiller() != context.victim()
                            && !gameWorld.isInnocent(context.serverKiller())
                            && GameFunctions.isPlayerAliveAndSurvival(context.serverKiller())
                            && !TimekeeperPlayerComponent.KEY.get(context.serverKiller()).isInTimeRift()) {
                        PlayerShopComponent.KEY.get(context.serverKiller()).addToBalance(100);
                    }
                }
        );
    }
}
