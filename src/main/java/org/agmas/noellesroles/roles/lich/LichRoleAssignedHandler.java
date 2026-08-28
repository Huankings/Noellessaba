package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * 巫妖职业分配后的初始化。
 */
public final class LichRoleAssignedHandler {
    private LichRoleAssignedHandler() {
    }

    public static void onRoleAssigned(@NotNull PlayerEntity player, @NotNull Role role) {
        if (role != NoellesRoleRegistry.LICH) {
            return;
        }

        /*
         * 巫妖控门术使用 NoellesRoles 通用 AbilityPlayerComponent。
         * 分配职业时直接覆盖通用开局冷却，确保 HUD 和服务端能力键从 45 秒开始一致倒计时。
         */
        AbilityPlayerComponent.KEY.get(player).setCooldown(LichConstants.DOOR_CONTROL_START_COOLDOWN_TICKS);
    }
}
