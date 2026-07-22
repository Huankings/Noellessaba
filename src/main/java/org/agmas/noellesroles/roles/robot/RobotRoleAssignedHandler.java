package org.agmas.noellesroles.roles.robot;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 机器人职业分配初始化。
 */
public final class RobotRoleAssignedHandler {
    private RobotRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != Noellesroles.ROBOT) {
            return;
        }

        /*
         * 机器人只有能力冷却，没有专属手持物。
         * 这里显式覆盖通用冷却为机器人自己的 30 秒开局冷却，便于后续单独调数值。
         */
        RobotPlayerComponent.KEY.get(player).reset();
        AbilityPlayerComponent.KEY.get(player).setCooldown(RobotConstants.START_COOLDOWN_TICKS);
    }
}
