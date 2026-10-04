package org.agmas.noellesroles.roles.perceiver;

import dev.doctor4t.wathe.api.combat.GunCooldownContext;
import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 通过 Wathe 枪击 API 固定感知者普通左轮的开枪后冷却。 */
public final class PerceiverGunCooldownHandler {
    private static boolean initialized;

    private PerceiverGunCooldownHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerCooldownModifier(
                NoellesRolesCore.id("perceiver_revolver_cooldown"),
                PerceiverConstants.REVOLVER_COOLDOWN_PRIORITY,
                PerceiverGunCooldownHandler::modifyCooldown
        );
    }

    private static int modifyCooldown(GunCooldownContext context, int currentCooldown) {
        if (!context.stack().isOf(WatheItems.REVOLVER)
                || context.shooter().isCreative()
                || !GameWorldComponent.KEY.get(context.shooter().getWorld())
                .isRole(context.shooter(), NoellesRoleRegistry.PERCEIVER)) {
            return currentCooldown;
        }
        return PerceiverConstants.REVOLVER_COOLDOWN_TICKS;
    }
}
