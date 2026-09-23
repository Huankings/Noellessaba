package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 迈尔斯确认击杀后的恶意值奖励。 */
public final class MyersDeathHandler {
    private static boolean initialized;

    private MyersDeathHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        DeathApi.registerAfterAttempt(NoellesRolesCore.id("myers/kill_malice"), DeathApi.PRIORITY_POST_CONFIRMED_DEATH, context -> {
            if (!context.confirmedDeath()
                    || !(context.killer() instanceof ServerPlayerEntity killer)
                    || GameWorldComponent.KEY.get(killer.getWorld()).getRole(killer) != NoellesRoleRegistry.MYERS) {
                return;
            }
            PlayerShopComponent.KEY.get(killer).addCurrencyAmount(
                    MyersConstants.MALICE_CURRENCY_ID, MyersConstants.KILL_MALICE_AMOUNT
            );
        });
    }
}
