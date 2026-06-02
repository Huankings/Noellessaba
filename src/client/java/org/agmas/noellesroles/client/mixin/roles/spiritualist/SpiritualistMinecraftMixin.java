package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵术师特殊相机状态下的本地交互拦截。
 */
@Mixin(MinecraftClient.class)
public abstract class SpiritualistMinecraftMixin {

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockDetachedAttack(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritualistClientController.isProjectionActive() || SpiritualistClientController.isPossessionViewActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockDetachedUse(CallbackInfo ci) {
        if (SpiritualistClientController.isProjectionActive()) {
            ci.cancel();
            return;
        }

        if (SpiritualistClientController.isPossessionViewActive()) {
            SpiritualistClientController.handleImmediatePossessionUseAttempt();
            ci.cancel();
        }
    }

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockProjectionPick(CallbackInfo ci) {
        if (SpiritualistClientController.isProjectionActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockDetachedBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (SpiritualistClientController.isProjectionActive() || SpiritualistClientController.isPossessionViewActive()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleInputEvents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z", ordinal = 2),
            cancellable = true
    )
    private void noellesroles$blockProjectionHotbar(CallbackInfo ci) {
        if (SpiritualistClientController.isProjectionActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "disconnect()V", at = @At("HEAD"))
    private void noellesroles$cleanupDetachedCameraOnDisconnect(CallbackInfo ci) {
        SpiritualistClientController.reset();
    }
}
