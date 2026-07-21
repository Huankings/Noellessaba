package org.agmas.noellesroles.roles.muzzler;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 静语者职业分配初始化。
 */
public final class MuzzlerRoleAssignedHandler {
    private MuzzlerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != Noellesroles.MUZZLER) {
            return;
        }

        // 静语者自身也可能在上一局被贴过胶带，分配新职业时先清掉受害者状态。
        SilencePlayerComponent.KEY.get(player).reset();
    }
}
