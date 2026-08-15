package org.agmas.noellesroles.client.mixin.roles.jason;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.agmas.noellesroles.roles.jason.JasonAbilitySoundRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无恶不在期间客户端实体声音兜底。
 *
 * <p>服务端 Entity#playSound 取消是主防线；这里再拦客户端收到的实体声音包，
 * 防止其它模组、资源重载或客户端预测路径把杰森的受伤 / 燃烧 / 水声重新播放出来。</p>
 */
@Mixin(ClientWorld.class)
public abstract class JasonAbilityClientWorldSoundMixin {
    @Inject(
            method = "playSoundFromEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$skipJasonAbilityClientEntitySound(
            Entity source,
            SoundEvent sound,
            SoundCategory category,
            float volume,
            float pitch,
            CallbackInfo ci
    ) {
        if (JasonAbilitySoundRules.shouldSuppressClientEntitySound(source, sound)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "playSoundFromEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/sound/SoundCategory;FFJ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$skipJasonAbilityClientEntitySound(
            PlayerEntity player,
            Entity source,
            RegistryEntry<SoundEvent> sound,
            SoundCategory category,
            float volume,
            float pitch,
            long seed,
            CallbackInfo ci
    ) {
        if (JasonAbilitySoundRules.shouldSuppressClientEntitySound(source, sound.value())) {
            ci.cancel();
        }
    }
}
