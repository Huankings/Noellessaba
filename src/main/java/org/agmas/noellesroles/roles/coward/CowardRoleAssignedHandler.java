package org.agmas.noellesroles.roles.coward;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 胆小鬼职业分配处理器。
 */
public final class CowardRoleAssignedHandler {
    private CowardRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.COWARD)) {
            return;
        }

        CowardPlayerComponent.KEY.get(player).reset();
        SedativePlayerComponent.KEY.get(player).reset();
    }
}
