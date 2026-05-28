package org.agmas.noellesroles.client.mixin.roles.stunned;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.roles.capture.StunnedPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public class StunnedKeyBindingMixin {
    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    private void wasPressed(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            StunnedPlayerComponent stunned = StunnedPlayerComponent.KEY.get(client.player);
            if (stunned.isStunned()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void isPressed(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            StunnedPlayerComponent stunned = StunnedPlayerComponent.KEY.get(client.player);
            if (stunned.isStunned()) {
                cir.setReturnValue(false);
            }
        }
    }
}