package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 酸液移速加成的 Wathe 移动 API 接入。 */
public final class KillerQueenMovementHandler {
    private KillerQueenMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(
                NoellesRolesCore.id("killer_queen/acid_speed"),
                PlayerMovementApi.DEFAULT_PRIORITY,
                context -> KillerQueenPlayerComponent.KEY.get(context.player()).hasSpeedBonus()
                        ? PlayerMovementApi.MovementSpeedResult.multiply((float) KillerQueenConstants.ACID_SPEED_MULTIPLIER)
                        : PlayerMovementApi.MovementSpeedResult.pass()
        );
    }
}
