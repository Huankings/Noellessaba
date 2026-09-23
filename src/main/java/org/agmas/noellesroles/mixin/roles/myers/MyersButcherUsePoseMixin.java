package org.agmas.noellesroles.mixin.roles.myers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.myers.MyersPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 服务端与客户端共同维持屠刀强制举起状态，确保自己和其他玩家看到同一动作。 */
@Mixin(LivingEntity.class)
public abstract class MyersButcherUsePoseMixin {
    @Inject(method = "isUsingItem", at = @At("HEAD"), cancellable = true)
    private void noellesroles$keepButcherUsing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (shouldHold(entity)) cir.setReturnValue(true);
    }

    @Inject(method = "getActiveItem", at = @At("HEAD"), cancellable = true)
    private void noellesroles$useButcherAsActiveItem(CallbackInfoReturnable<ItemStack> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (shouldHold(entity)) cir.setReturnValue(entity.getMainHandStack());
    }

    @Inject(method = "getActiveHand", at = @At("HEAD"), cancellable = true)
    private void noellesroles$useMainHandForButcher(CallbackInfoReturnable<Hand> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (shouldHold(entity)) cir.setReturnValue(Hand.MAIN_HAND);
    }

    private static boolean shouldHold(LivingEntity entity) {
        return entity instanceof PlayerEntity
                && entity.getMainHandStack().isOf(ModItems.BUTCHER_KNIFE)
                && MyersPlayerComponent.KEY.get(entity).isForcedHold();
    }
}
