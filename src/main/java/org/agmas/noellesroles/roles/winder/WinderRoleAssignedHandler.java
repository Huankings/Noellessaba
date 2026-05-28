package org.agmas.noellesroles.roles.winder;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 风灵师职业分配处理器。
 */
public final class WinderRoleAssignedHandler {

    private WinderRoleAssignedHandler() {
    }

    /**
     * 旧实现里风灵师拿到职业时只做一件事：
     * 重置自己的风灵师组件状态。
     *
     * <p>能力冷却则继续沿用前面统一写入的通用基线，不在这里额外覆盖。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.WINDER)) {
            return;
        }

        WinderPlayerComponent.KEY.get(player).reset();
    }
}
