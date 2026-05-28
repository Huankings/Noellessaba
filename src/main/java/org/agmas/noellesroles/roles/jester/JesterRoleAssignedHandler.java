package org.agmas.noellesroles.roles.jester;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 狂信者职业分配处理器。
 */
public final class JesterRoleAssignedHandler {

    private JesterRoleAssignedHandler() {
    }

    /**
     * 保留旧开局物资：
     * 假匕首、假左轮、撬棍。
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.JESTER)) {
            return;
        }

        player.giveItemStack(ModItems.FAKE_KNIFE.getDefaultStack());
        player.giveItemStack(ModItems.FAKE_REVOLVER.getDefaultStack());
        player.giveItemStack(WatheItems.CROWBAR.getDefaultStack());
    }
}
