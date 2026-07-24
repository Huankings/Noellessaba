package org.agmas.noellesroles.roles.mimic;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;

/**
 * 模仿者职业分配处理器。
 */
public final class MimicRoleAssignedHandler {

    private MimicRoleAssignedHandler() {
    }

    /**
     * 保留旧逻辑：模仿者开局获得一把假匕首。
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.MIMIC)) {
            return;
        }

        player.giveItemStack(ModItems.FAKE_KNIFE.getDefaultStack());
    }
}
