package org.agmas.noellesroles.roles.necromancer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;

/**
 * 死灵法师职业分配初始化。
 */
public final class NecromancerRoleAssignedHandler {
    private NecromancerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.NECROMANCER) {
            return;
        }

        /*
         * NoellesRoles 会先给所有职业套一层通用开局能力冷却。
         * StupidExpress 的死灵法师开局没有冷却，只在成功复活后进入 3 分钟冷却，
         * 因此这里必须显式清零，避免搬运后开局手感发生变化。
         */
        AbilityPlayerComponent.KEY.get(player).setCooldown(0);
    }
}
