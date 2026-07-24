package org.agmas.noellesroles.roles.starstruck;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;

/**
 * 星界使者职业分配初始化。
 */
public final class StarstruckRoleAssignedHandler {
    private StarstruckRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.STARSTRUCK) {
            return;
        }

        StarstruckPlayerComponent.KEY.get(player).reset();
        /*
         * NoellesRoles 会先给所有能力职业写入通用开局冷却。
         * 星界使者在 StarryExpress 里开局就是 0 冷却，用户也确认要保留这个原行为，
         * 所以这里必须在职业专属初始化阶段覆盖掉通用值。
         */
        AbilityPlayerComponent.KEY.get(player).setCooldown(StarstruckConstants.START_COOLDOWN_TICKS);
    }
}
