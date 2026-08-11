package org.agmas.noellesroles.client.mixin.roles.spring_trap;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapSoundRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Presence Footsteps 兼容层。
 *
 * <p>Presence Footsteps 会绕过 ClientWorld#playSoundFromEntity，直接通过自己的 SoundEngine 播放脚步。
 * 使用 @Pseudo + require=0 可以在未安装该模组时安全跳过；安装时则把弹簧陷阱状态玩家的脚步音量归零。</p>
 */
@Pseudo
@Mixin(targets = "eu.ha3.presencefootsteps.sound.SoundEngine", remap = false)
public abstract class SpringTrapPresenceFootstepsVolumeMixin {
    @Inject(
            method = "getVolumeForSource",
            at = @At("HEAD"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void noellesroles$suppressSpringTrapPresenceFootstepsVolume(
            LivingEntity source,
            CallbackInfoReturnable<Float> cir
    ) {
        if (source instanceof PlayerEntity player && SpringTrapSoundRules.shouldSuppressSounds(player)) {
            cir.setReturnValue(0.0F);
        }
    }
}
