package org.agmas.noellesroles.mixin.compat;

import dev.doctor4t.wathe.cca.WorldBlackoutComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 用于把 Wathe 停电组件中的剩余 tick 直接归零，
 * 这样工程师恢复电力后，停电状态判定也会同步结束。
 */
@Mixin(WorldBlackoutComponent.class)
public interface WorldBlackoutComponentAccessor {

    @Accessor("ticks")
    void noellesroles$setTicks(int ticks);
}
