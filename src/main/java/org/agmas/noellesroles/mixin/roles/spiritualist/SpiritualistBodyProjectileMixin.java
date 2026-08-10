package org.agmas.noellesroles.mixin.roles.spiritualist;

import net.minecraft.entity.Entity;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistBodyRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵术师附身本体的原版投射物命中兜底。
 *
 * <p>玩家之间的碰撞 / 推挤已经接入 Wathe {@code PlayerCollisionApi}；
 * 玩家渲染、准心、右键交互和攻击已经接入 Wathe {@code TargetVisibilityApi}。
 * 但原版 {@code canBeHitByProjectile()} 没有攻击者上下文，暂时不能自然映射到 TargetVisibilityApi，
 * 所以这里只保留一个很窄的 mixin，专门防止附身期间的隐藏本体被箭、雪球等原版投射物命中。</p>
 */
@Mixin(Entity.class)
public abstract class SpiritualistBodyProjectileMixin {
    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hidePossessingBodyFromProjectiles(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritualistBodyRules.isPossessingBody((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
