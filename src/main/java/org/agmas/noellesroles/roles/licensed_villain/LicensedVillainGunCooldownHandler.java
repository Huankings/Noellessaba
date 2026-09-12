package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.combat.GunCooldownContext;
import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 执照恶棍时刻的 Wathe 左轮冷却修正。 */
public final class LicensedVillainGunCooldownHandler {
    private static boolean initialized;

    private LicensedVillainGunCooldownHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerCooldownModifier(
                NoellesRolesCore.id("licensed_villain_moment_revolver_cooldown"),
                GunShotApi.DEFAULT_PRIORITY + 100,
                LicensedVillainGunCooldownHandler::modifyCooldown
        );
    }

    private static int modifyCooldown(GunCooldownContext context, int currentCooldown) {
        if (!context.stack().isOf(WatheItems.REVOLVER)
                || !dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(context.shooter().getWorld())
                .isRole(context.shooter(), NoellesRoleRegistry.LICENSED_VILLAIN)) {
            return currentCooldown;
        }
        return LicensedVillainMomentWorldComponent.KEY.get(context.shooter().getWorld())
                .isActive(context.shooter().getUuid())
                ? LicensedVillainConstants.MOMENT_REVOLVER_COOLDOWN_TICKS
                : currentCooldown;
    }
}
