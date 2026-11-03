package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.item.GrenadeItem;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 假手雷。
 *
 * <p>直投/蓄力、实体生成、统计、投掷回放和物品消耗全部复用 Wathe {@link GrenadeItem}。
 * 这里只把真实物品写进飞行实体，爆炸时由 {@link FakeGrenadeExplosionHandler} 识别并取消伤害。</p>
 */
public class FakeGrenadeItem extends GrenadeItem {
    public FakeGrenadeItem(Settings settings) {
        super(settings);
    }

    @Override
    protected @NotNull ItemStack createProjectileStack(@NotNull ItemStack sourceStack) {
        return sourceStack.copyWithCount(1);
    }
}
