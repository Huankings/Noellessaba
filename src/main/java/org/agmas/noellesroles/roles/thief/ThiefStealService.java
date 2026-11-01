package org.agmas.noellesroles.roles.thief;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.random.Random;

/** 小偷空手偷窃与悦灵偷窃共用的背包槽位服务。 */
public final class ThiefStealService {
    private ThiefStealService() {
    }

    /**
     * 沿用现有小偷语义：只扫描玩家主背包和快捷栏，不扫描护甲与副手，
     * 并且成功后拿走被选中槽位里的整个物品栈。
     */
    public static @NotNull List<Integer> getStealableSlots(@NotNull ServerPlayerEntity target) {
        List<Integer> slots = new ArrayList<>();
        for (int index = 0; index < target.getInventory().main.size(); index++) {
            ItemStack stack = target.getInventory().main.get(index);
            if (!stack.isEmpty() && ThiefItemRules.canTake(stack.getItem())) {
                slots.add(index);
            }
        }
        return slots;
    }

    /**
     * 从目标实时背包重新选择一个合法槽位并移除整个栈。
     * 多只悦灵同 tick 抵达时会逐只调用，因此后执行的悦灵一定能看见前一只已经造成的背包变化。
     */
    public static @Nullable ItemStack stealRandomStack(
            @NotNull ServerPlayerEntity target,
            @NotNull Random random
    ) {
        List<Integer> slots = getStealableSlots(target);
        if (slots.isEmpty()) {
            return null;
        }

        int selectedSlot = slots.get(random.nextInt(slots.size()));
        ItemStack stolen = target.getInventory().main.get(selectedSlot).copy();
        target.getInventory().main.set(selectedSlot, ItemStack.EMPTY);
        target.getInventory().markDirty();
        target.currentScreenHandler.sendContentUpdates();
        return stolen;
    }
}
