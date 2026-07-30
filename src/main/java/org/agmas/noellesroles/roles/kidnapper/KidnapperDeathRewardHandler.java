package org.agmas.noellesroles.roles.kidnapper;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

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

        DeathApi.registerBeforeAttempt(
                NoellesRolesCore.id("kidnapper_controlled_kill_reward"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    if (context.killer() == null) {
                        return;
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    KidnapperComponent controlled = KidnapperComponent.KEY.get(context.victim());
                    if (gameWorld.isRole(context.killer(), NoellesRoleRegistry.KIDNAPPER) && controlled.controlTicks > 0) {
                        /*
                         * 这里保留旧 mixin 的“死亡请求入口发放”语义。
                         * controlled.controlTicks 表示受害者仍处于绑匪迷药控制窗口，
                         * 因此奖励只归属给亲自造成这次死亡请求的绑匪。
                         *
                         * 如果未来希望改成确认死亡后奖励，可以直接把注册阶段迁到 afterAttempt。
                         */
                        PlayerShopComponent.KEY.get(context.killer()).addToBalance(KidnapperConstants.ADDITIONAL_KILL_REWARD_COINS);
                    }
                }
        );
    }
}
