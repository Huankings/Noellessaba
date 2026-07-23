package org.agmas.noellesroles.mixin.roles.convener;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取单条物品冷却记录的结束 tick。
 *
 * <p>该内部类在 Yarn 环境里不是公共 API，因此不能在普通源码里直接声明类型。
 * 用 targets 字符串可以保持 accessor 很窄，只暴露召集者计算剩余冷却所需字段。</p>
 */
@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownEntryAccessor {
    /**
     * 当前冷却记录在哪个 tick 结束。
     */
    @Accessor("endTick")
    int noellesroles$getEndTick();
}
