package org.agmas.noellesroles.roles.operator;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;

/**
 * 接线员职业分配处理器。
 */
public final class OperatorRoleAssignedHandler {

    private OperatorRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.OPERATOR)) {
            return;
        }

        // 沿用 noellesroles 现有职业分配习惯：
        // 1. 把主动能力冷却回到通用初始值；
        // 2. 清空上一局残留的接线 / 广播状态。
        AbilityPlayerComponent.KEY.get(player).setCooldown(NoellesRolesConfig.HANDLER.instance().generalCooldownTicks);
        OperatorPlayerComponent.KEY.get(player).reset();
    }
}
