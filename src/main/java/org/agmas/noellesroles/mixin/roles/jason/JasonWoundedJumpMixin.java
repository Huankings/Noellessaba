package org.agmas.noellesroles.mixin.roles.jason;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonWoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 杰森重伤倒地期间禁止自主跳跃。
 *
 * <p>移动速度已经走 Wathe 的 PlayerMovementApi，这里只补一个很窄的 jump 入口兜底：
 * 客户端会先拦跳跃键，但服务端仍必须防止延迟包或异常客户端直接触发跳跃。</p>
 */
@Mixin(LivingEntity.class)
public abstract class JasonWoundedJumpMixin {
    @Shadow
    protected boolean jumping;

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedJump(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && JasonWoundManager.isWoundedActionLocked(player)) {
            this.jumping = false;
            ci.cancel();
        }
    }
}
