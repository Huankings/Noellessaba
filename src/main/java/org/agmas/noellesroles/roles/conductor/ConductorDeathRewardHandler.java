package org.agmas.noellesroles.roles.conductor;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

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

        DeathApi.registerBeforeAttempt(
                NoellesRolesCore.id("conductor_death_reward"),
                DeathApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * 这里刻意保留旧 MoneyIncreaseMixin 的“死亡请求入口发钱”语义。
                     * 它不是通用击杀确认奖励，而是列车长被袭击时的旧版即时收益；
                     * 如果后续要改成严格确认死亡后再奖励，可以整体迁到 afterAttempt 并检查 confirmedDeath()。
                     */
                    if (context.killer() == null) {
                        return;
                    }
                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(context.victim().getWorld());
                    if (gameWorld.isRole(context.victim(), NoellesRoleRegistry.CONDUCTOR)
                            && !gameWorld.isInnocent(context.killer())) {
                        PlayerShopComponent.KEY.get(context.killer()).addToBalance(100);
                    }
                }
        );
    }
}
