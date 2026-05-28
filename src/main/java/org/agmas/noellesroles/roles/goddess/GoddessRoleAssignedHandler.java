package org.agmas.noellesroles.roles.goddess;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;

/**
 * 圣母职业分配处理器。
 */
public final class GoddessRoleAssignedHandler {

    private GoddessRoleAssignedHandler() {
    }

    /**
     * 旧实现里圣母会显式调用一次 {@code setCooldown(generalCooldownTicks)}。
     *
     * <p>虽然前面的通用基线已经把字段写成相同值，
     * 这里仍要保留，因为它会额外触发同步，这属于旧语义的一部分。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.GODDESS)) {
            return;
        }

        AbilityPlayerComponent.KEY.get(player).setCooldown(NoellesRolesConfig.HANDLER.instance().generalCooldownTicks);
    }
}
