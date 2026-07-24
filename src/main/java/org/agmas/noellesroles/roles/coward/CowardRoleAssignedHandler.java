package org.agmas.noellesroles.roles.coward;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 胆小鬼职业分配处理器。
 */
public final class CowardRoleAssignedHandler {
    private CowardRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.COWARD)) {
            return;
        }

        CowardPlayerComponent.KEY.get(player).reset();
        SedativePlayerComponent.KEY.get(player).reset();
    }
}
