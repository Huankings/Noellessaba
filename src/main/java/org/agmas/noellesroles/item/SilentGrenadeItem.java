package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.item.GrenadeItem;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 无声手雷。
 *
 * <p>公共投掷流程会按本物品在 GameConstants 中登记的五分钟冷却自动写入冷却，
 * 并把这里返回的真实物品栈交给 AssassinGrenadeHandler 做无声爆炸与回放。</p>
 */
public class SilentGrenadeItem extends GrenadeItem {
    public SilentGrenadeItem(Settings settings) {
        super(settings);
    }

    @Override
    protected @NotNull ItemStack createProjectileStack(@NotNull ItemStack sourceStack) {
        return sourceStack.copyWithCount(1);
    }
}
