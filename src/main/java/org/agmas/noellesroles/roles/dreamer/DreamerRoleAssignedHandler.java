package org.agmas.noellesroles.roles.dreamer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;

/**
 * 梦者职业分配初始化。
 */
public final class DreamerRoleAssignedHandler {
    private DreamerRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (!role.equals(NoellesRoleRegistry.DREAMER)) {
            return;
        }

        DreamerComponent.KEY.get(player).reset();
        DreamerKillerComponent dreamerProgress = DreamerKillerComponent.KEY.get(player);
        dreamerProgress.reset();
        dreamerProgress.setDreamerRequired();

        /*
         * 梦之印记的开局数量现在跟本局杀手人数动态挂钩：
         * 1 个杀手时不给印记，之后每多 1 个杀手多给 1 个，最多 3 个。
         * 这里在职业分配事件里计算，是因为 Harpy 已经先写入了本局杀手位；
         * 计算逻辑内部也有 ready player count 兜底，避免强制职业或调试局读到异常 0。
         */
        int imprintCount = DreamerConstants.getInitialDreamImprintCount(player);
        if (imprintCount > 0) {
            player.giveItemStack(new ItemStack(ModItems.DREAM_IMPRINT, imprintCount));
        }
    }
}
