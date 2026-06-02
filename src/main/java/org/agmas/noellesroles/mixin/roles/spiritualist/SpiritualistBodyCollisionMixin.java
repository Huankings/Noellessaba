package org.agmas.noellesroles.mixin.roles.spiritualist;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵术师本体在脱体状态下的碰撞与交互处理。
 *
 * <p>用户要求分成两种表现：</p>
 * <p>1. 出窍时，本体仍然可见、可受伤，但不能被实体推开；</p>
 * <p>2. 附身时，本体要像空气一样不可见、不可选中、不可交互、不可被实体挤走。</p>
 */
@Mixin(Entity.class)
public abstract class SpiritualistBodyCollisionMixin {

    @WrapMethod(method = "collidesWith")
    private boolean noellesroles$ignoreDetachedBodyEntityCollision(Entity other, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity selfPlayer) {
            if (SpiritualistPlayerComponent.KEY.get(selfPlayer).hasDetachedBodyState()) {
                return false;
            }
        }
        if (other instanceof PlayerEntity otherPlayer) {
            if (SpiritualistPlayerComponent.KEY.get(otherPlayer).hasDetachedBodyState()) {
                return false;
            }
        }
        return original.call(other);
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void noellesroles$makeDetachedBodyNotPushable(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player
                && SpiritualistPlayerComponent.KEY.get(player).hasDetachedBodyState()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void noellesroles$makeDetachedBodyNonCollidable(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player
                && SpiritualistPlayerComponent.KEY.get(player).hasDetachedBodyState()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hidePossessingBodyFromTargeting(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player
                && SpiritualistPlayerComponent.KEY.get(player).isPossessing()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hidePossessingBodyFromProjectiles(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player
                && SpiritualistPlayerComponent.KEY.get(player).isPossessing()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hidePossessingBodyFromAllPlayers(
            PlayerEntity player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof PlayerEntity spiritualist
                && SpiritualistPlayerComponent.KEY.get(spiritualist).isPossessing()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockPossessingBodyInteract(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if ((Object) this instanceof PlayerEntity spiritualist
                && SpiritualistPlayerComponent.KEY.get(spiritualist).isPossessing()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockPossessingBodyInteractAt(
            PlayerEntity player,
            Vec3d hitPos,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if ((Object) this instanceof PlayerEntity spiritualist
                && SpiritualistPlayerComponent.KEY.get(spiritualist).isPossessing()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
