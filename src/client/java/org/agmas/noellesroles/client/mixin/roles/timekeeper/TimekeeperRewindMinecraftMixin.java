package org.agmas.noellesroles.client.mixin.roles.timekeeper;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperRewindInputLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 回溯期间兜底拦截 MinecraftClient 的实际交互入口。
 *
 * <p>KeyBinding 和 Mouse mixin 负责清理输入状态；这里再挡住 doAttack / doItemUse 等最终入口，
 * 是为了处理回溯开始那一帧已经排队到 MinecraftClient 内部的点击，避免它绕过按键层继续执行。</p>
 */
@Mixin(MinecraftClient.class)
public abstract class TimekeeperRewindMinecraftMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void noellesroles$clearRewindInputAtTickStart(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (TimekeeperRewindInputLock.isInputLocked(client)) {
            TimekeeperRewindInputLock.releaseBlockedKeys(client);
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRewindAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TimekeeperRewindInputLock.isInputLocked(client)) {
            return;
        }

        TimekeeperRewindInputLock.releaseBlockedKeys(client);
        cir.setReturnValue(false);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRewindItemUse(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TimekeeperRewindInputLock.isInputLocked(client)) {
            return;
        }

        TimekeeperRewindInputLock.releaseBlockedKeys(client);
        ci.cancel();
    }

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRewindItemPick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TimekeeperRewindInputLock.isInputLocked(client)) {
            return;
        }

        TimekeeperRewindInputLock.releaseBlockedKeys(client);
        ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRewindBlockBreaking(boolean breaking, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!TimekeeperRewindInputLock.isInputLocked(client)) {
            return;
        }

        TimekeeperRewindInputLock.releaseBlockedKeys(client);
        ci.cancel();
    }
}
