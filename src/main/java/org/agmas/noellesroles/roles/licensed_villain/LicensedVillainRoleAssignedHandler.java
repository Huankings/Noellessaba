package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 执照恶棍职业分配后的开局处理。
 */
public final class LicensedVillainRoleAssignedHandler {
    private LicensedVillainRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.LICENSED_VILLAIN)) {
            return;
        }

        // 保留 kinssaba 原行为：执照恶棍开局自带一个 Wathe 原版开锁器。
        player.giveItemStack(WatheItems.LOCKPICK.getDefaultStack());
    }
}
