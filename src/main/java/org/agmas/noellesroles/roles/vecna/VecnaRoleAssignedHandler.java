package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.api.Role;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 维克那分配职业时设置开局 30 秒技能冷却并清除旧标记。 */
public final class VecnaRoleAssignedHandler {
    private VecnaRoleAssignedHandler() {}
    public static void onRoleAssigned(net.minecraft.entity.player.PlayerEntity player, Role role) {
        VecnaPlayerComponent.KEY.get(player).reset();
        if (role == NoellesRoleRegistry.VECNA) {
            AbilityPlayerComponent.KEY.get(player).setCooldown(VecnaConstants.ABILITY_START_COOLDOWN_TICKS);
        }
    }
}
