package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.combat.KnifeCooldownApi;
import dev.doctor4t.wathe.api.combat.KnifeCooldownContext;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 亡命徒存活时保持匕首无冷却。 */
public final class OutlawKnifeCooldownHandler {
    private static final int PRIORITY = 200;
    private static boolean initialized;

    private OutlawKnifeCooldownHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        KnifeCooldownApi.registerCooldownModifier(
                NoellesRolesCore.id("outlaw/knife_cooldown"),
                PRIORITY,
                OutlawKnifeCooldownHandler::modify
        );
    }

    private static int modify(KnifeCooldownContext context, int currentCooldown) {
        return context.game().isRole(context.attacker(), NoellesRoleRegistry.OUTLAW) ? 0 : currentCooldown;
    }
}
