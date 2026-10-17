package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.api.combat.KnifeCooldownApi;
import dev.doctor4t.wathe.api.combat.KnifeCooldownContext;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 维克那用匕首杀死自己领取标记目标时免除冷却。 */
public final class VecnaKnifeCooldownHandler {
    private static final int PRIORITY = 200;
    private static boolean initialized;

    private VecnaKnifeCooldownHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        KnifeCooldownApi.registerCooldownModifier(
                NoellesRolesCore.id("vecna/marked_knife_cooldown"),
                PRIORITY,
                VecnaKnifeCooldownHandler::modify
        );
    }

    private static int modify(KnifeCooldownContext context, int currentCooldown) {
        VecnaPlayerComponent mark = VecnaPlayerComponent.KEY.get(context.target());
        if (context.game().isRole(context.attacker(), NoellesRoleRegistry.VECNA)
                && mark.isMarked()
                && context.attacker().getUuid().equals(mark.getMarker())) {
            return 0;
        }
        return currentCooldown;
    }
}
