package org.agmas.noellesroles.roles.bomber;

import dev.doctor4t.wathe.api.death.DeathApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 炸弹持有者死亡后的炸弹清理与奖励。
 */
public final class BomberDeathHandler {
    private static boolean initialized = false;

    private BomberDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerAfterMarkedDead(
                NoellesRolesCore.id("bomber_carrier_death"),
                DeathApi.DEFAULT_PRIORITY,
                /*
                 * 炸弹携带者死亡清理放在 afterMarkedDead：
                 * 此时玩家已经确认死亡并切旁观，但尸体、掉落和心情重置还没发生，
                 * BomberPlayerComponent 可以安全地停止携带状态、结算爆炸/传递相关奖励。
                 */
                context -> BomberPlayerComponent.handleBombCarrierDeath(context.victim(), context.deathReason())
        );
    }
}
