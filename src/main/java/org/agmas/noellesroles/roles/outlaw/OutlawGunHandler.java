package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.combat.GunShotApi;
import dev.doctor4t.wathe.api.combat.GunCooldownContext;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyContext;
import dev.doctor4t.wathe.api.combat.RevolverPenaltyResult;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 亡命徒的左轮冷却和无辜目标惩罚豁免。 */
public final class OutlawGunHandler {
    private static boolean initialized;

    private OutlawGunHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        GunShotApi.registerCooldownModifier(NoellesRolesCore.id("outlaw_revolver_cooldown"), 200, OutlawGunHandler::modifyCooldown);
        GunShotApi.registerInnocentRevolverPenaltyRule(NoellesRolesCore.id("outlaw_revolver_penalty"), 200, OutlawGunHandler::resolvePenalty);
    }

    private static int modifyCooldown(GunCooldownContext context, int current) {
        return context.stack().isOf(WatheItems.REVOLVER)
                && GameWorldComponent.KEY.get(context.shooter().getWorld()).isRole(context.shooter(), NoellesRoleRegistry.OUTLAW)
                ? OutlawConstants.REVOLVER_COOLDOWN_TICKS
                : current;
    }

    private static RevolverPenaltyResult resolvePenalty(RevolverPenaltyContext context) {
        return GameWorldComponent.KEY.get(context.shooter().getWorld()).isRole(context.shooter(), NoellesRoleRegistry.OUTLAW)
                ? RevolverPenaltyResult.SKIP
                : RevolverPenaltyResult.PASS;
    }
}
