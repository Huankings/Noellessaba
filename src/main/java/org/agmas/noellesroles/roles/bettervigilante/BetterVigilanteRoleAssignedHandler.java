package org.agmas.noellesroles.roles.bettervigilante;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 更好的义警职业分配处理器。
 */
public final class BetterVigilanteRoleAssignedHandler {

    private BetterVigilanteRoleAssignedHandler() {
    }

    /**
     * 保留旧逻辑：更好的义警开局获得一颗手雷。
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.BETTER_VIGILANTE)) {
            return;
        }

        player.giveItemStack(WatheItems.GRENADE.getDefaultStack());
    }
}
