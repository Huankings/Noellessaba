package org.agmas.noellesroles.mixin.roles.jason;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.roles.jason.JasonAbilitySoundRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无恶不在期间吞掉杰森自己发出的暴露性声音。
 *
 * <p>原版游泳声、入水 splash、泡泡柱等路径最终都会落到 Entity#playSound(sound, volume, pitch)。
 * 受伤音也通常会从实体自身播放。这里统一取消可以同时挡住服务端广播和客户端本地预测声，
 * 避免幽魂杰森在水中或被异常伤害路径命中时被声音暴露位置。</p>
 */
@Mixin(Entity.class)
public abstract class JasonAbilityEntitySoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipJasonAbilityWaterSound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player && JasonAbilitySoundRules.shouldSuppressAbilityEntitySound(player, sound)) {
            ci.cancel();
        }
    }
}
