package org.agmas.noellesroles.roles.dreamer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;

/**
 * 梦者职业分配初始化。
 */
public final class DreamerRoleAssignedHandler {
    private DreamerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.DREAMER)) {
            return;
        }

        DreamerComponent.KEY.get(player).reset();
        DreamerKillerComponent dreamerProgress = DreamerKillerComponent.KEY.get(player);
        dreamerProgress.reset();
        dreamerProgress.setDreamerRequired();
        player.giveItemStack(new ItemStack(ModItems.DREAM_IMPRINT, DreamerConstants.INITIAL_DREAM_IMPRINT_COUNT));
    }
}
