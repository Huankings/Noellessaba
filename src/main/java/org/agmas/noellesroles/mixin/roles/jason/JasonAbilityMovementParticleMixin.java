package org.agmas.noellesroles.mixin.roles.jason;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 无恶不在期间取消杰森自己的移动粒子。
 */
@Mixin(Entity.class)
public abstract class JasonAbilityMovementParticleMixin {
    @Inject(method = "shouldSpawnSprintingParticles", at = @At("HEAD"), cancellable = true)
    private void noellesroles$disableJasonAbilitySprintingParticles(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player && JasonAbilityRules.isAbilityActiveLike(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "spawnSprintingParticles", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipJasonAbilitySprintingParticles(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && JasonAbilityRules.isAbilityActiveLike(player)) {
            ci.cancel();
        }
    }
}
