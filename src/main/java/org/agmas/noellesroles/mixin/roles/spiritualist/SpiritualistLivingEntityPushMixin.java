package org.agmas.noellesroles.mixin.roles.spiritualist;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistBodyRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 覆盖活体推挤入口，确保灵术师脱体本体不会产生任何原版轻微推挤。
 *
 * <p>{@code LivingEntity#pushAway(Entity)} 内部会调用对方的 {@code Entity#pushAwayFrom(Entity)}。
 * 虽然 {@link SpiritualistBodyCollisionMixin} 已经在 {@code pushAwayFrom} 做了兜底，
 * 但这里提前取消一次，可以让“玩家主动撞向灵术师本体”和“灵术师本体被别的活体扫描到”
 * 两种路径都明确落到同一条规则上，避免只取消碰撞箱却留下速度反推。</p>
 */
@Mixin(LivingEntity.class)
public abstract class SpiritualistLivingEntityPushMixin {
    @Inject(method = "pushAway(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipDetachedBodyLivingPush(Entity other, CallbackInfo ci) {
        if (SpiritualistBodyRules.shouldIgnorePlayerBodyCollision((Entity) (Object) this, other)) {
            ci.cancel();
        }
    }
}
