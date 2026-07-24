package org.agmas.noellesroles.roles.magician;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;

/**
 * 魔术师职业分配处理器。
 */
public final class MagicianRoleAssignedHandler {

    private MagicianRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.MAGICIAN)) {
            return;
        }

        MagicianPlayerComponent.KEY.get(player).reset();
        AbilityPlayerComponent.KEY.get(player).setCooldown(MagicianConstants.INITIAL_COOLDOWN_TICKS);
    }
}
