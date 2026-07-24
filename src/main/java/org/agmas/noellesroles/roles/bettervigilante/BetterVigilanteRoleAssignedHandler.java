package org.agmas.noellesroles.roles.bettervigilante;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;

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
        if (!role.equals(NoellesRoleRegistry.BETTER_VIGILANTE)) {
            return;
        }

        player.giveItemStack(WatheItems.GRENADE.getDefaultStack());
    }
}
