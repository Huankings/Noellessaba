package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 亡命徒职业分配后的物品和组件初始化。 */
public final class OutlawRoleAssignedHandler {
    private OutlawRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.OUTLAW) {
            return;
        }
        player.giveItemStack(WatheItems.KNIFE.getDefaultStack());
        player.giveItemStack(WatheItems.REVOLVER.getDefaultStack());
        player.giveItemStack(WatheItems.DERRINGER.getDefaultStack());
        player.giveItemStack(WatheItems.CROWBAR.getDefaultStack());
    }
}
