package org.agmas.noellesroles.roles.arsonist;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

/**
 * 纵火犯开局物品发放。
 */
public final class ArsonistRoleAssignedHandler {
    private ArsonistRoleAssignedHandler() {
    }

    public static void onRoleAssigned(@NotNull PlayerEntity player, @NotNull Role role) {
        if (!role.equals(NoellesRoleRegistry.ARSONIST)) {
            return;
        }

        player.giveItemStack(ModItems.JERRY_CAN.getDefaultStack());
        player.giveItemStack(ModItems.LIGHTER.getDefaultStack());
        player.giveItemStack(WatheItems.CROWBAR.getDefaultStack());
    }
}
