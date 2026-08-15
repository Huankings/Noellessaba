package org.agmas.noellesroles.mixin.roles.jason;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 杰森重伤倒地期间禁止重新拾取被迫掉落的左轮手枪。
 *
 * <p>左轮掉落后是普通 ItemEntity，原版拾取逻辑不会区分玩家是否处于杰森倒地状态，
 * 因此必须在 ItemEntity#onPlayerCollision 入口拦截。这里只限制左轮，背包整理和其它
 * 地面物品仍保持原有行为，并且其它存活玩家仍可以正常捡走这把枪。</p>
 */
@Mixin(ItemEntity.class)
public abstract class JasonWoundedRevolverPickupMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockWoundedRevolverPickup(PlayerEntity player, CallbackInfo ci) {
        if (JasonWoundManager.shouldBlockRevolverPickup(player, (ItemEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
