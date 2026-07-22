package org.agmas.noellesroles.client.mixin.roles.kidnapper;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class KidnapperControlledMouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockKidnappedScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !WatheClient.isPlayerAliveAndInSurvival()) {
            return;
        }

        if (KidnapperComponent.KEY.get(client.player).controlTicks > 0) {
            /*
             * 鼠标滚轮可以直接切换热键栏槽位，不经过 KeyBinding。
             * 被迷药控制期间必须一并取消，避免目标在黑屏/跟随状态下预先滚到枪、刀等反制物品。
             */
            ci.cancel();
        }
    }
}
