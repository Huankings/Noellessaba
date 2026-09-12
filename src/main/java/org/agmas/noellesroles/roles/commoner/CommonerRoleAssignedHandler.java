package org.agmas.noellesroles.roles.commoner;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.outlaw.OutlawPlayerComponent;

/** 平民职业分配后的状态清理。 */
public final class CommonerRoleAssignedHandler {
    private CommonerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role == NoellesRoleRegistry.COMMONER) {
            OutlawPlayerComponent.KEY.get(player).reset();
        }
    }
}
