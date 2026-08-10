package org.agmas.noellesroles.roles.starstruck;

import dev.doctor4t.wathe.api.movement.PlayerMovementApi;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 星界使者能力期间的固定移速覆盖。
 */
public final class StarstruckMovementHandler {
    private static final int PRIORITY = 1500;

    private StarstruckMovementHandler() {
    }

    public static void init() {
        PlayerMovementApi.registerSpeedModifier(NoellesRolesCore.id("movement/starstruck_ability"), PRIORITY, context -> {
            PlayerEntity player = context.player();
            if (!StarstruckConstants.ABILITY_AFFECTS_MOVEMENT_SPEED
                    || !context.gameWorld().isRole(player, NoellesRoleRegistry.STARSTRUCK)
                    || StarstruckPlayerComponent.KEY.get(player).ticks <= 0) {
                return PlayerMovementApi.MovementSpeedResult.pass();
            }

            /*
             * Starstruck 原本就是固定速度值，而不是乘当前速度。
             * 因此这里继续使用 override；优先级低于常规倍率，使该能力保持“星界固定手感”。
             */
            return PlayerMovementApi.MovementSpeedResult.override(context.sprinting()
                    ? StarstruckConstants.ABILITY_SPRINT_SPEED
                    : StarstruckConstants.ABILITY_WALK_SPEED);
        });
    }
}
