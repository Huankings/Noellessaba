package org.agmas.noellesroles.client.mixin.roles.muzzler;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbientLoop;
import dev.doctor4t.ratatouille.client.util.ambience.BackgroundAmbience;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import org.agmas.noellesroles.roles.muzzler.MuzzlerPsychoUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 已经播放中的 psycho_drone，也要在“只剩静语者疯魔”时淡出。
 */
@Mixin(value = BackgroundAmbientLoop.class, priority = 1500)
public class MuzzlerPsychoDroneLoopMixin {
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/ratatouille/client/util/ambience/BackgroundAmbience$PlayPredicate;shouldPlay(Lnet/minecraft/client/network/ClientPlayerEntity;)Z"
            )
    )
    private boolean noellesroles$fadeOutPsychoDroneForMuzzler(BackgroundAmbience.PlayPredicate predicate,
                                                              ClientPlayerEntity player,
                                                              Operation<Boolean> original) {
        boolean shouldPlay = original.call(predicate, player);
        if (!shouldPlay) {
            return false;
        }

        BackgroundAmbientLoop loop = (BackgroundAmbientLoop) (Object) this;
        if (!loop.getId().equals(Registries.SOUND_EVENT.getId(WatheSounds.AMBIENT_PSYCHO_DRONE))) {
            return true;
        }

        return MuzzlerPsychoUtil.hasNonMuzzlerPsycho(player.getWorld());
    }
}
