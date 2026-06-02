package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 灵术师职业分配处理器。
 *
 * <p>灵术师在开局时只需要重置自身状态；
 * 初始技能冷却则继续沿用前面统一写入的通用能力冷却。</p>
 */
public final class SpiritualistRoleAssignedHandler {
    private SpiritualistRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.SPIRITUALIST)) {
            return;
        }

        SpiritualistPlayerComponent.KEY.get(player).reset();
        SpiritualistHostComponent.KEY.get(player).reset();
    }
}
