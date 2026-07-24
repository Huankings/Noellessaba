package org.agmas.noellesroles.roles.cleaner;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;

/**
 * 清道夫职业分配初始化。
 */
public final class CleanerRoleAssignedHandler {
    private CleanerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.CLEANER) {
            return;
        }

        // kinssaba 原逻辑：清道夫开局自带一个硫酸桶，用来清理尸体并获得金币。
        player.giveItemStack(ModItems.SULFURIC_ACID_BARREL.getDefaultStack());
    }
}
