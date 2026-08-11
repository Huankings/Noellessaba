package org.agmas.noellesroles.mixin.roles.spring_trap;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapDoorSoundContext;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapSoundRules;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 Wathe 门交互调用栈内吞掉普通门声。
 *
 * <p>选择 World#playSound 而不是改 DoorBlockEntity，是因为钥匙/开锁器/锁门提示音在门方块 onUse 里直接播放，
 * 它们不经过 DoorBlockEntity#playToggleSound。这里通过 SpringTrapDoorSoundContext 保证只影响当前开门者。</p>
 */
@Mixin(World.class)
public abstract class SpringTrapDoorWorldSoundMixin {
    @Inject(
            method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$suppressSpringTrapDoorSounds(
            @Nullable PlayerEntity except,
            double x,
            double y,
            double z,
            SoundEvent sound,
            SoundCategory category,
            float volume,
            float pitch,
            CallbackInfo ci
    ) {
        if (category == SoundCategory.BLOCKS
                && SpringTrapSoundRules.shouldSuppressDoorSound(sound)
                && SpringTrapDoorSoundContext.shouldSuppressForCurrentOpener((World) (Object) this)) {
            ci.cancel();
        }
    }
}
