package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 小偷开局能力冷却。
 */
public final class ThiefRoleAssignedHandler {
    private ThiefRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role.equals(NoellesRoleRegistry.THIEF)) {
            /*
             * 按 StupidExpress 源码迁移：小偷开局时偷窃能力先进入完整 70 秒冷却。
             */
            AbilityPlayerComponent.KEY.get(player).setCooldown(ThiefConstants.STEAL_COOLDOWN_TICKS);
        }
    }
}
