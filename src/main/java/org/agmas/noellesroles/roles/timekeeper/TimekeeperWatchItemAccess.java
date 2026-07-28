package org.agmas.noellesroles.roles.timekeeper;

import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.jetbrains.annotations.NotNull;

/**
 * 怀表回放用的小工具。
 *
 * <p>这个类存在只是为了让回放 helper 不直接依赖物品类的展示细节：
 * 记录时保存稳定状态 id，formatter 再把状态 id 解析成对应物品名。</p>
 */
final class TimekeeperWatchItemAccess {
    private TimekeeperWatchItemAccess() {
    }

    static String stateId(@NotNull ItemStack stack) {
        return TimekeeperWatchItem.getState(stack).id();
    }
}
