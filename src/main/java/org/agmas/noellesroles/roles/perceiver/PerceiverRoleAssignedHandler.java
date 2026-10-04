package org.agmas.noellesroles.roles.perceiver;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 感知者职业分配后的局内状态初始化。 */
public final class PerceiverRoleAssignedHandler {
    private PerceiverRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role == NoellesRoleRegistry.PERCEIVER) {
            /* 通用开局冷却已由职业分配总引导写入，这里只负责清除上一局的感知状态。 */
            PerceiverPlayerComponent.KEY.get(player).reset();
        }
    }
}
