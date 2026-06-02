package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 被附身者本地不能切换热键栏，也不能再借由滚轮触发额外交互。
 */
@Mixin(Mouse.class)
public abstract class SpiritualistPossessedMouseMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockPossessedScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && SpiritualistHostComponent.KEY.get(client.player).possessed) {
            ci.cancel();
        }
    }
}
