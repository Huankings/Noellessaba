package org.agmas.noellesroles.mixin.roles.jason;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.agmas.noellesroles.roles.jason.JasonAbilitySoundRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无恶不在期间 LivingEntity 单参声音兜底。
 *
 * <p>大多数受伤音会走 Entity#playSound(sound, volume, pitch)，但原版和其它扩展也可能调用
 * LivingEntity#playSound(sound) 这个重载。这里复用同一套声音 id 过滤，避免扣血 / 灼烧音从重载路径漏出。</p>
 */
@Mixin(LivingEntity.class)
public abstract class JasonAbilityLivingEntitySoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;)V", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipJasonAbilityLivingEntitySound(SoundEvent sound, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && JasonAbilitySoundRules.shouldSuppressAbilityEntitySound(player, sound)) {
            ci.cancel();
        }
    }

    @Inject(method = "playBlockFallSound", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipJasonAbilityBlockFallSound(CallbackInfo ci) {
        /*
         * 原版高处落地会走 LivingEntity#playBlockFallSound 播放方块 fallSound。
         * 这条路径不一定带有普通 step/walk/run 标记，所以在方法入口直接按无恶不在状态取消，
         * 避免杰森从高处落地时通过方块落地声暴露位置。
         */
        if ((Object) this instanceof PlayerEntity player && JasonAbilityRules.isAbilityActiveLike(player)) {
            ci.cancel();
        }
    }
}
