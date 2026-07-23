package org.agmas.noellesroles.roles.convener;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

/**
 * 召集者开局状态初始化。
 */
public final class ConvenerRoleAssignedHandler {
    private ConvenerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(@NotNull PlayerEntity player, @NotNull Role role) {
        if (!role.equals(Noellesroles.CONVENER)) {
            return;
        }

        player.giveItemStack(WatheItems.LOCKPICK.getDefaultStack());

        ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(player);
        convener.initializeForRole();
        convener.setRequiredSummons(ConvenerWinHelper.getRequiredSummons(player.getWorld()));
        convener.sync();

        ConvenerDisguiseComponent.KEY.get(player).clearDisguise();
        ConvenerMomentumComponent.KEY.get(player).reset();
        AbilityPlayerComponent.KEY.get(player).setCooldown(0);
    }
}
