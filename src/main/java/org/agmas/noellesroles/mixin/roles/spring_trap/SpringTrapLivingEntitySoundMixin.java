package org.agmas.noellesroles.mixin.roles.spring_trap;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapSoundRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 弹簧陷阱移动音的 LivingEntity 兜底。
 *
 * <p>部分脚步路径不会走 Entity#playStepSound，而是最终落到 LivingEntity#playSound。
 * 这里只按声音 id 里的 step/walk/run 等移动标记过滤，不会吞掉彩虹斧命中、撬门、爆炸或疯魔环境音。</p>
 */
@Mixin(LivingEntity.class)
public abstract class SpringTrapLivingEntitySoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;)V", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipSpringTrapMovementSound(SoundEvent sound, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && SpringTrapSoundRules.shouldSuppressMovementSound(player, sound)) {
            ci.cancel();
        }
    }
}
