package org.agmas.noellesroles.roles.mimic;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

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
        if (!role.equals(Noellesroles.MIMIC)) {
            return;
        }

        player.giveItemStack(ModItems.FAKE_KNIFE.getDefaultStack());
    }
}
