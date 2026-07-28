package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 时停者职业分配处理。
 */
public final class TimekeeperRoleAssignedHandler {
    private TimekeeperRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.TIMEKEEPER)) {
            return;
        }

        TimekeeperPlayerComponent.KEY.get(player).onAssigned();
        player.giveItemStack(TimekeeperWatchItem.createStack(TimekeeperWatchState.NORMAL));
    }
}
