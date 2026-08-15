package org.agmas.noellesroles.mixin.roles.jason;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无恶不在期间阻止原版火焰状态写入。
 *
 * <p>仅取消“把火焰 tick 设为正数”的调用，允许 setFireTicks(0) 正常通过。
 * 这样火焰、岩浆、燃烧箭等原版路径无法让幽魂杰森身上残留燃烧特效，
 * 但能力结束、回合清理或其它系统主动清火仍能照常工作。</p>
 */
@Mixin(Entity.class)
public abstract class JasonAbilityFireMixin {
    @Inject(method = "setFireTicks", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipJasonAbilityFireTicks(int fireTicks, CallbackInfo ci) {
        if (fireTicks > 0
                && (Object) this instanceof PlayerEntity player
                && JasonAbilityRules.isAbilityActiveLike(player)) {
            ci.cancel();
        }
    }
}
