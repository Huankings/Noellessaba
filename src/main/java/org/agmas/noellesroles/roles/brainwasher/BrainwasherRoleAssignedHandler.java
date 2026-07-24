package org.agmas.noellesroles.roles.brainwasher;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;

/**
 * 洗脑师职业分配处理器。
 */
public final class BrainwasherRoleAssignedHandler {

    private BrainwasherRoleAssignedHandler() {
    }

    /**
     * 旧实现里洗脑师开局能力冷却为 0，并立即同步到客户端。
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.BRAINWASHER)) {
            return;
        }

        AbilityPlayerComponent.KEY.get(player).setCooldown(0);
    }
}
