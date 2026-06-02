package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 统一处理灵术师的鼠标视角重定向与锁定。
 */
@Mixin(Entity.class)
public abstract class SpiritualistEntityInputMixin {

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void noellesroles$redirectLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || (Object) this != client.player) {
            return;
        }

        if (SpiritualistClientController.redirectDetachedLook(cursorDeltaX, cursorDeltaY)) {
            ci.cancel();
            return;
        }

        if (SpiritualistHostComponent.KEY.get(client.player).possessed) {
            ci.cancel();
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void noellesroles$preventProjectionCameraPush(Entity entity, CallbackInfo ci) {
        if (!SpiritualistClientController.isProjectionActive()) {
            return;
        }

        Entity spiritCamera = SpiritualistClientController.getProjectionCamera();
        if (spiritCamera != null && (entity == spiritCamera || (Object) this == spiritCamera)) {
            ci.cancel();
        }
    }
}
