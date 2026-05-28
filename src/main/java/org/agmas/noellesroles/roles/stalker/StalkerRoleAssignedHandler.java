package org.agmas.noellesroles.roles.stalker;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 潜行者职业分配处理器。
 */
public final class StalkerRoleAssignedHandler {

    private StalkerRoleAssignedHandler() {
    }

    /**
     * 潜行者在拿到职业时，会重置整套阶段状态，
     * 并额外把 {@code isStalkerMarked} 标成 true。
     *
     * <p>这个标记在现有实现里不是多余字段，
     * 它被用来识别“这个玩家当前确实已经成为潜行者”，
     * 所以后续迁移时也要原样保留。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.STALKER)) {
            return;
        }

        StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(player);
        comp.reset();
        comp.isStalkerMarked = true;
        comp.sync();
    }
}
