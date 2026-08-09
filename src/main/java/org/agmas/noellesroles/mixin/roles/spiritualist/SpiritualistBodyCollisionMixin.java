package org.agmas.noellesroles.mixin.roles.spiritualist;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistBodyRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵术师本体在脱体状态下的碰撞与交互处理。
 *
 * <p>用户要求分成两种表现：</p>
 * <p>1. 出窍时，本体仍然可见、可受伤，但不能被实体推开；</p>
 * <p>2. 附身时，本体要像空气一样不可见、不可选中、不可交互、不可被实体挤走。</p>
 *
 * <p>这里故意使用较低 priority。Wathe 会在自己的 {@code Entity#collidesWith}
 * wrapper 中把局内存活玩家互相碰撞强制改成 {@code true}；本 mixin 需要在那之后再包一层，
 * 对灵术师脱体本体做最终否决，否则 Wathe 的返回值会把本体重新变成实体墙。</p>
 */
@Mixin(value = Entity.class, priority = 100)
public abstract class SpiritualistBodyCollisionMixin {

    @WrapMethod(method = "collidesWith")
    private boolean noellesroles$ignoreDetachedBodyEntityCollision(Entity other, Operation<Boolean> original) {
        Entity self = (Entity) (Object) this;
        if (SpiritualistBodyRules.shouldIgnorePlayerBodyCollision(self, other)) {
            /*
             * 这里是玩家互挡的最终兜底：无论原版还是 Wathe 先前怎么判断，
             * 只要任意一方是灵术师脱体本体，就不能再产生实体碰撞箱。
             */
            return false;
        }
        return original.call(other);
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void noellesroles$makeDetachedBodyNotPushable(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritualistBodyRules.isDetachedBody((Entity) (Object) this)) {
            // 脱体本体自身不应被原版推挤系统当作“可推动实体”。
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void noellesroles$makeDetachedBodyNonCollidable(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritualistBodyRules.isDetachedBody((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipDetachedBodyPushAwayFrom(Entity other, CallbackInfo ci) {
        if (SpiritualistBodyRules.shouldIgnorePlayerBodyCollision((Entity) (Object) this, other)) {
            /*
             * 原版 pushAwayFrom 会给双方各加一小段水平速度。
             * 附身/出窍本体要求完全没有推挤手感，所以这里直接取消整个推挤入口。
             */
            ci.cancel();
        }
    }

    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hidePossessingBodyFromTargeting(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritualistBodyRules.isPossessingBody((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hidePossessingBodyFromProjectiles(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritualistBodyRules.isPossessingBody((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hidePossessingBodyFromAllPlayers(
            PlayerEntity player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (SpiritualistBodyRules.isPossessingBody((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockPossessingBodyInteract(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (SpiritualistBodyRules.isPossessingBody((Entity) (Object) this)) {
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
        if (SpiritualistBodyRules.isPossessingBody((Entity) (Object) this)) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
