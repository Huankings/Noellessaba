package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 迈尔斯屠刀冲刺的服务端速度修正。 */
public final class MyersMovementHandler {
    private MyersMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(NoellesRolesCore.id("myers/butcher_dash"),
                MyersConstants.DASH_SPEED_PRIORITY, context -> {
                    MyersPlayerComponent component = MyersPlayerComponent.KEY.get(context.player());
                    if (!component.isDashing() || !context.sprinting()) {
                        return PlayerMovementApi.MovementSpeedResult.pass();
                    }
                    return PlayerMovementApi.MovementSpeedResult.multiply(MyersConstants.BUTCHER_KNIFE_DASH_SPEED_MULTIPLIER);
                });
    }
}
