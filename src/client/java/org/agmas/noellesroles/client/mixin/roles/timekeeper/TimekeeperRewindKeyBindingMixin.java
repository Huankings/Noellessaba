package org.agmas.noellesroles.client.mixin.roles.timekeeper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperRewindInputLock;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 回溯期间清理 KeyBinding 层残留输入。
 *
 * <p>这里放在较高 priority，是为了早于普通眩晕/控制类 mixin 处理：
 * 这些 mixin 只会把返回值改成 false，而时间回溯还必须主动清空按键内部队列，
 * 否则回溯结束后旧的 wasPressed 计数会继续触发短按。</p>
 */
@Mixin(value = KeyBinding.class, priority = 5000)
public abstract class TimekeeperRewindKeyBindingMixin {
    @Unique
    private void noellesroles$blockRewindKey(@NotNull CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        KeyBinding key = (KeyBinding) (Object) this;
        if (!TimekeeperRewindInputLock.shouldBlockKey(client, key)) {
            return;
        }

        TimekeeperRewindInputLock.releaseBlockedKeys(client);
        cir.setReturnValue(false);
    }

    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRewindWasPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        noellesroles$blockRewindKey(cir);
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRewindIsPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        noellesroles$blockRewindKey(cir);
    }
}
