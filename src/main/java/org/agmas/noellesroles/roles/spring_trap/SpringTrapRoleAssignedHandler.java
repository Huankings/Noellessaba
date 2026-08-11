package org.agmas.noellesroles.roles.spring_trap;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 弹簧陷阱职业分配初始化。
 */
public final class SpringTrapRoleAssignedHandler {
    private SpringTrapRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.SPRING_TRAP) {
            return;
        }

        /*
         * 血斧是弹簧陷阱商店中替换匕首的核心武器。
         * 冷却写在职业分配阶段，而不是购买阶段，是为了表达“开局 30 秒后才可使用”：
         * 玩家如果开局立刻买到血斧，需要等剩余开局冷却；如果 30 秒后才购买，则不会额外等待。
         */
        SpringTrapPlayerComponent springTrap = SpringTrapPlayerComponent.KEY.get(player);
        springTrap.reset();
        springTrap.startRoundCooldowns();

        player.getItemCooldownManager().set(ModItems.BLOOD_AXE, SpringTrapConstants.BLOOD_AXE_START_COOLDOWN_TICKS);
    }
}
