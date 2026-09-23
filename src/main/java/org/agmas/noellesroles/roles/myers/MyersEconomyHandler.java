package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 迈尔斯任务完成后的恶意值奖励。 */
public final class MyersEconomyHandler {
    private static boolean initialized;

    private MyersEconomyHandler() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            if (!(context.player() instanceof ServerPlayerEntity player)
                    || GameWorldComponent.KEY.get(player.getWorld()).getRole(player) != NoellesRoleRegistry.MYERS
                    || !GameFunctions.isPlayerAliveAndSurvival(player)) {
                return;
            }
            PlayerShopComponent.KEY.get(player).addCurrencyAmount(
                    MyersConstants.MALICE_CURRENCY_ID, MyersConstants.TASK_MALICE_AMOUNT
            );
        });
    }
}
