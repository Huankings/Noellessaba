package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 灵术师附身时，把原本属于本体的移动输入截下来改送给宿主。
 */
@Mixin(KeyboardInput.class)
public abstract class SpiritualistKeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void noellesroles$capturePossessionMovement(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (!SpiritualistClientController.shouldCapturePossessionMovement()) {
            return;
        }

        SpiritualistClientController.capturePossessionMovement((Input) (Object) this);
    }
}
