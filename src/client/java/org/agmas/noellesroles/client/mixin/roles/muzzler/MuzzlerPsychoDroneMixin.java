package org.agmas.noellesroles.client.mixin.roles.muzzler;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbience;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.Registries;
import org.agmas.noellesroles.roles.muzzler.MuzzlerPsychoUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 阻止“只有静语者在疯魔”时启动 psycho_drone 背景音。
 */
@Mixin(value = BackgroundAmbience.class, priority = 1500)
public abstract class MuzzlerPsychoDroneMixin {
    @Shadow @Final
    private BackgroundAmbience.SoundFactory factory;

    @WrapOperation(
            method = "tryStarting",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/ratatouille/client/util/ambience/BackgroundAmbience$PlayPredicate;shouldPlay(Lnet/minecraft/client/network/ClientPlayerEntity;)Z"
            )
    )
    private boolean noellesroles$mutePsychoDroneForMuzzler(BackgroundAmbience.PlayPredicate predicate,
                                                           ClientPlayerEntity player,
                                                           Operation<Boolean> original) {
        boolean shouldPlay = original.call(predicate, player);
        if (!shouldPlay) {
            return false;
        }

        SoundInstance soundInstance = this.factory.create(player);
        if (soundInstance == null
                || !soundInstance.getId().equals(Registries.SOUND_EVENT.getId(WatheSounds.AMBIENT_PSYCHO_DRONE))) {
            return true;
        }

        /*
         * psycho_drone 是全局环境音。只看本地玩家身份会误判：
         * 旁人听到的声音也要在“场上只有静语者疯魔”时被静音。
         */
        return MuzzlerPsychoUtil.hasNonMuzzlerPsycho(player.getWorld());
    }
}
