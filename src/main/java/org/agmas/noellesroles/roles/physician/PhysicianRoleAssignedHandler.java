package org.agmas.noellesroles.roles.physician;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

/**
 * 医师职业分配后的初始状态。
 */
public final class PhysicianRoleAssignedHandler {
    private PhysicianRoleAssignedHandler() {
    }

    public static void onRoleAssigned(@NotNull PlayerEntity player, @NotNull Role role) {
        if (!role.equals(NoellesRoleRegistry.PHYSICIAN)) {
            return;
        }

        /*
         * 医师开局携带医疗箱；药丸护盾状态则一定重置，避免换职业或新回合继承上一把护盾。
         */
        PhysicianPlayerComponent.KEY.get(player).reset();
        player.giveItemStack(ModItems.MEDICAL_KIT.getDefaultStack());
    }
}
