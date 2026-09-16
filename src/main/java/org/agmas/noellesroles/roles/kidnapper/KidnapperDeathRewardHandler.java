package org.agmas.noellesroles.roles.kidnapper;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;

/**
 * 绑匪击杀迷药控制中的目标时的额外金币。
 */
public final class KidnapperDeathRewardHandler {
    private static boolean initialized = false;

    private KidnapperDeathRewardHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerAfterMarkedDead(
                NoellesRolesCore.id("kidnapper_controlled_kill_reward"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH + 40,
                context -> {
                    /* markedDead 只会在 Wathe 真正切换为死亡旁观后触发，护盾/免死不会进入此阶段。 */
                    if (!context.markedDead() || !context.victimAliveAtStart() || context.serverKiller() == null) {
                        return;
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    KidnapperComponent controlled = KidnapperComponent.KEY.get(context.victim());
                    if (gameWorld.isRole(context.serverKiller(), NoellesRoleRegistry.KIDNAPPER)
                            && GameFunctions.isPlayerAliveAndSurvival(context.serverKiller())
                            && !TimekeeperPlayerComponent.KEY.get(context.serverKiller()).isInTimeRift()
                            && controlled.controlTicks > 0
                            && context.serverKiller().getUuid().equals(controlled.controllerUUID)) {
                        /*
                         * controlled.controlTicks 和 controllerUUID 同时匹配，确保奖励只归属
                         * 亲自控制并完成击杀的绑匪；confirmedDeath() 保证护盾/免死不会产生奖励。
                         */
                        PlayerShopComponent.KEY.get(context.killer()).addToBalance(KidnapperConstants.ADDITIONAL_KILL_REWARD_COINS);
                    }
                }
        );
    }
}
