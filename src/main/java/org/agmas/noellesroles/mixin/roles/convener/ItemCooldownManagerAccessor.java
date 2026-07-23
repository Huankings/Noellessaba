package org.agmas.noellesroles.mixin.roles.convener;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 读取原版物品冷却管理器的内部状态。
 *
 * <p>召集者封控需要“只延长较短冷却，不覆盖更长冷却”，而原版公开 API 只提供
 * 是否在冷却中和百分比进度，没有直接暴露剩余 tick。这里用一个很窄的 accessor
 * 只读取冷却表和当前 tick，避免为了单个职业去改 Wathe 或复制大段冷却逻辑。</p>
 */
@Mixin(ItemCooldownManager.class)
public interface ItemCooldownManagerAccessor {
    /**
     * 当前玩家身上的全部物品冷却记录。
     */
    @Accessor("entries")
    Map<Item, Object> noellesroles$getEntries();

    /**
     * ItemCooldownManager 自己维护的计时 tick。
     */
    @Accessor("tick")
    int noellesroles$getTick();
}
