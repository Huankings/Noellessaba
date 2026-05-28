package org.agmas.noellesroles.client.mixin.roles.controller;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class ControlledMouseMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.isAlive()) {
            ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(client.player);
            if (controlledComp.isControlled) {
                ci.cancel(); // 取消鼠标滚轮事件，阻止物品栏切换
            }
        }
    }
}