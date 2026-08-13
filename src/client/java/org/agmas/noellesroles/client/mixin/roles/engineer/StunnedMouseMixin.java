package org.agmas.noellesroles.client.mixin.roles.engineer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.agmas.noellesroles.client.roles.engineer.StunnedInputLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 眩晕 / 定身期间拦截鼠标按钮与滚轮。
 *
 * <p>鼠标左/右键可能绑定到攻击、使用、选槽等 KeyBinding，但滚轮切槽不总是走 wasPressed。
 * 所以鼠标层命中眩晕锁时也要主动清理按键队列，避免定身结束后补攻击、补右键或补切槽。</p>
 */
@Mixin(Mouse.class)
public abstract class StunnedMouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (StunnedInputLock.shouldBlockMouseButton(client, button)) {
            StunnedInputLock.releaseBlockedKeys(client);
            ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (StunnedInputLock.isInputLocked(client)) {
            StunnedInputLock.releaseBlockedKeys(client);
            ci.cancel();
        }
    }
}
