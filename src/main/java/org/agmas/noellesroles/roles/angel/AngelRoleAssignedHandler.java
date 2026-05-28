package org.agmas.noellesroles.roles.angel;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.config.NoellesRolesConfig;

/**
 * 天使职业分配处理器。
 */
public final class AngelRoleAssignedHandler {

    private AngelRoleAssignedHandler() {
    }

    /**
     * 处理天使在职业分配瞬间需要完成的初始化。
     *
     * <p>这里保留旧实现的三个动作：</p>
     * <p>1. 把能力冷却设置为通用初始冷却，并立即同步；</p>
     * <p>2. 清空天使自身上一局残留的守护 / 安抚状态；</p>
     * <p>3. 不再在这里动态注入欢迎公告，避免与现有公告链路互相覆盖。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.ANGEL)) {
            return;
        }

        AbilityPlayerComponent.KEY.get(player).setCooldown(NoellesRolesConfig.HANDLER.instance().generalCooldownTicks);
        AngelPlayerComponent.KEY.get(player).reset();
    }
}
