package org.agmas.noellesroles.mixin.modifiers.allergic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.agmas.noellesroles.modifiers.allergic.AllergicConsumeHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 过敏患者唯一需要的窄 mixin：监听物品完成食用/饮用。
 *
 * <p>Wathe 目前没有公开“玩家刚吃下某个 ItemStack”的事件接口；
 * 因此这里仿照 Noelles 厨师的食用监听挂到 {@link Item#finishUsing}，
 * 真正的过敏类型判断和副作用仍交给独立 handler，避免把玩法逻辑写进 mixin。</p>
 */
@Mixin(Item.class)
public abstract class AllergicFinishUsingMixin {
    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void noellesroles$triggerAllergicConsume(
            @NotNull ItemStack stack,
            @NotNull World world,
            @NotNull LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
            return;
        }

        AllergicConsumeHandler.handleConsume(player, stack, world);
    }
}
