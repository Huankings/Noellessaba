package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 亡命徒 2.8 倍移速。 */
public final class OutlawMovementHandler {
    private static boolean initialized;

    private OutlawMovementHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        PlayerMovementApi.registerSpeedModifier(NoellesRolesCore.id("movement/outlaw"), 100, context ->
                context.role() == NoellesRoleRegistry.OUTLAW && context.gameWorld().isRole(context.player(), NoellesRoleRegistry.OUTLAW)
                        ? PlayerMovementApi.MovementSpeedResult.multiply(OutlawConstants.SPEED_MULTIPLIER)
                        : PlayerMovementApi.MovementSpeedResult.pass()
        );
    }
}
