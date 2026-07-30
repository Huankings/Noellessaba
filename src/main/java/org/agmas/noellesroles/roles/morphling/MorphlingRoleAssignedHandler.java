package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 变形怪职业分配初始化。
 */
public final class MorphlingRoleAssignedHandler {
    private MorphlingRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.MORPHLING) {
            return;
        }

        /*
         * 当前 NoellesRoles 已经有一套背包按钮主动变形组件；
         * 试剂增强是第二套可购买机制，所以这里只清理试剂标记并补发遥控器，
         * 不改原主动变形的 1 分 10 秒持续和 5 秒冷却。
         */
        if (player instanceof ServerPlayerEntity serverPlayer) {
            MorphlingReagentService.reset(serverPlayer);
            MorphlingReagentService.assignForRole(serverPlayer, role);
        }
    }
}
