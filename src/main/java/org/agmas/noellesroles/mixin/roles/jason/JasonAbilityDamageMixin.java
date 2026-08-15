package org.agmas.noellesroles.mixin.roles.jason;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonAbilityManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无恶不在期间的原版伤害兜底。
 *
 * <p>Wathe 的枪、刀、职业击杀最终都会进入 DeathApi；但原版或其它扩展的伤害可能只是先扣血，
 * 并不一定马上触发 Wathe 死亡流程。这里拦截无恶不在杰森受到的原版普通伤害，
 * 只保留 /kill 对应的 Generic Kill，方便管理员测试死亡清理。</p>
 */
@Mixin(LivingEntity.class)
public abstract class JasonAbilityDamageMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonAbilitySurvivalDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity target && JasonAbilityManager.shouldCancelAbilityDamage(target, source)) {
            cir.setReturnValue(false);
        }
    }
}
