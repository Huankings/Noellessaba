package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 保持灵魂出窍期间本地玩家的基础客户端逻辑继续推进。
 *
 * <p>这和 spark 版灵界行者一样，让原版继续把本地玩家当作“当前相机宿主”，
 * 避免切到灵魂相机后某些只在 camera player 上运行的逻辑直接停掉。</p>
 */
@Mixin(ClientPlayerEntity.class)
public abstract class SpiritualistLocalPlayerMixin {

    @Inject(method = "isCamera", at = @At("HEAD"), cancellable = true)
    private void noellesroles$keepLocalPlayerAsCamera(CallbackInfoReturnable<Boolean> cir) {
        if ((SpiritualistClientController.isProjectionActive() || SpiritualistClientController.isPossessionViewActive())
                && (Object) this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
