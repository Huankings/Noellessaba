package org.agmas.noellesroles.mixin.roles.cook;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.cook.CookPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 标记刚吃下食物的玩家，供厨师客户端本能高亮。
 */
@Mixin(Item.class)
public abstract class CookFinishEatMixin {
    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void noellesroles$markPlayerAfterEating(
            @NotNull ItemStack stack,
            @NotNull World world,
            @NotNull LivingEntity entity,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (world.isClient || !(entity instanceof PlayerEntity player)) {
            return;
        }
        if (stack.getItem().getUseAction(stack) == UseAction.EAT) {
            /*
             * 不在这里判断是否存在厨师：写一次短状态成本很低，
             * 而客户端是否显示由 CookInstinctHandler 再按观察者职业精确判断。
             */
            CookPlayerComponent.KEY.get(player).markAteFood();
        }
    }
}
