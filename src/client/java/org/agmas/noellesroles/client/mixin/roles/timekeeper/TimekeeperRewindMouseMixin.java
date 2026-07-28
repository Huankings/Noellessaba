package org.agmas.noellesroles.client.mixin.roles.timekeeper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperRewindInputLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 回溯期间拦截鼠标按钮与滚轮。
 *
 * <p>鼠标左/右键和滚轮并不总是通过 KeyBinding.wasPressed 进入游戏逻辑。
 * 尤其是滚轮会直接切换热键栏，所以这里单独拦截，避免回溯期间产生新的选槽或使用物品输入。</p>
 */
@Mixin(Mouse.class)
public abstract class TimekeeperRewindMouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRewindMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TimekeeperRewindInputLock.shouldBlockMouseButton(client, button)) {
            return;
        }

        TimekeeperRewindInputLock.releaseBlockedKeys(client);
        ci.cancel();
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRewindMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TimekeeperRewindInputLock.isInputLocked(client)) {
            return;
        }

        TimekeeperRewindInputLock.releaseBlockedKeys(client);
        ci.cancel();
    }
}
