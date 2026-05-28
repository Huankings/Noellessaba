package org.agmas.noellesroles.roles.controller;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 附体师职业分配处理器。
 */
public final class ControllerRoleAssignedHandler {

    private ControllerRoleAssignedHandler() {
    }

    /**
     * 旧实现里附体师拿到身份时，会把能力冷却直接改成 30 tick。
     *
     * <p>这里继续保留“直接写字段、不立刻 sync”的原始做法，
     * 以免影响其它依赖这一时机的代码路径。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.CONTROLLER)) {
            return;
        }

        AbilityPlayerComponent abilityPlayerComponent = AbilityPlayerComponent.KEY.get(player);
        abilityPlayerComponent.cooldown = 30;
    }
}
