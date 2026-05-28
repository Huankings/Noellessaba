package org.agmas.noellesroles.roles.prophet;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;

/**
 * 先知职业分配处理器。
 */
public final class ProphetRoleAssignedHandler {

    private ProphetRoleAssignedHandler() {
    }

    /**
     * 先知在职业分配时需要：
     * 清空上一局的标记 / 庇护状态，并显式同步一次通用能力冷却。
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.PROPHET)) {
            return;
        }

        ProphetPlayerComponent.KEY.get(player).reset();
        AbilityPlayerComponent.KEY.get(player).setCooldown(NoellesRolesConfig.HANDLER.instance().generalCooldownTicks);
    }
}
