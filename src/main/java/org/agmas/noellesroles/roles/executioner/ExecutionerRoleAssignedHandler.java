package org.agmas.noellesroles.roles.executioner;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;

/**
 * 仇杀客职业分配处理器。
 */
public final class ExecutionerRoleAssignedHandler {

    private ExecutionerRoleAssignedHandler() {
    }

    /**
     * 保留旧初始化顺序：
     * 先清空“是否已经赢过”的标记，再重置目标，并额外再同步一次。
     *
     * <p>虽然 {@code reset()} 本身已经会同步，
     * 这里仍然保留最后这次显式 {@code sync()}，确保与旧行为完全一致。</p>
     */
    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(Noellesroles.EXECUTIONER)) {
            return;
        }

        ExecutionerPlayerComponent executionerPlayerComponent = ExecutionerPlayerComponent.KEY.get(player);
        executionerPlayerComponent.won = false;
        executionerPlayerComponent.reset();
        executionerPlayerComponent.sync();
    }
}
