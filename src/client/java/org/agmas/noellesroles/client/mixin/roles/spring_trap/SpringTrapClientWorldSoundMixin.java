package org.agmas.noellesroles.client.mixin.roles.spring_trap;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapSoundRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端移动音兜底。
 *
 * <p>部分音频增强或客户端重放路径可能绕过服务端脚步取消；这里只吞玩家来源、PLAYERS 分类、
 * 且 id 看起来像 step/walk/run 的声音，不影响彩虹斧、撬门、爆炸等明确动作音。</p>
 */
@Mixin(ClientWorld.class)
public abstract class SpringTrapClientWorldSoundMixin {
    @Inject(
            method = "playSoundFromEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$skipSpringTrapClientEntityMovementSound(
            Entity source,
            SoundEvent sound,
            SoundCategory category,
            float volume,
            float pitch,
            CallbackInfo ci
    ) {
        if (SpringTrapSoundRules.shouldSuppressClientEntityMovementSound(source, sound, category)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "playSoundFromEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$skipSpringTrapClientEntityMovementSound(
            PlayerEntity player,
            Entity source,
            RegistryEntry<SoundEvent> sound,
            SoundCategory category,
            float volume,
            float pitch,
            long seed,
            CallbackInfo ci
    ) {
        if (SpringTrapSoundRules.shouldSuppressClientEntityMovementSound(source, sound.value(), category)) {
            ci.cancel();
        }
    }
}
