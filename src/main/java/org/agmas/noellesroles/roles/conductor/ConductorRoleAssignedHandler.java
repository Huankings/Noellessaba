package org.agmas.noellesroles.roles.conductor;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 列车长职业分配处理器。
 */
public final class ConductorRoleAssignedHandler {

    private ConductorRoleAssignedHandler() {
    }

    /**
     * 保留旧开局物资：
     * 万能钥匙、开锁器、假左轮。
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.CONDUCTOR)) {
            return;
        }

        player.giveItemStack(ModItems.MASTER_KEY.getDefaultStack());
        player.giveItemStack(WatheItems.LOCKPICK.getDefaultStack());
        player.giveItemStack(ModItems.FAKE_REVOLVER.getDefaultStack());
    }
}
