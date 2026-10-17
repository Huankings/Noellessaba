package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 杀手皇后开局能力冷却初始化。 */
public final class KillerQueenRoleAssignedHandler {
    private KillerQueenRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role == NoellesRoleRegistry.KILLER_QUEEN) {
            AbilityPlayerComponent.KEY.get(player).setCooldown(KillerQueenConstants.INITIAL_ABILITY_COOLDOWN_TICKS);
        }
    }
}
