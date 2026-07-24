package org.agmas.noellesroles.roles.hunter;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.ModItems;

/**
 * 追猎者职业分配初始化。
 */
public final class HunterRoleAssignedHandler {
    private HunterRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.HUNTER) {
            return;
        }

        /*
         * 上一局可能在举刀、释放窗口或临时冷却中结束。
         * 新一局分到追猎者时先 reset 清掉残留状态，再像强盗/刺客专属武器一样写入开局冷却。
         * 组件里的 startRoundCooldowns 只标记“这是 30 秒开局冷却”，实际禁用仍交给 ItemCooldownManager。
         */
        HunterPlayerComponent hunterComponent = HunterPlayerComponent.KEY.get(player);
        hunterComponent.reset();
        hunterComponent.startRoundCooldowns();

        player.getItemCooldownManager().set(ModItems.HUNTING_KNIFE, HunterConstants.HUNTING_KNIFE_START_COOLDOWN_TICKS);
    }
}
