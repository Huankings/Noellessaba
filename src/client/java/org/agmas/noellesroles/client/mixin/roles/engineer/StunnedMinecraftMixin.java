package org.agmas.noellesroles.client.mixin.roles.engineer;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.roles.engineer.StunnedInputLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 眩晕 / 定身期间兜底拦截 MinecraftClient 的实际交互入口。
 *
 * <p>KeyBinding 和 Mouse mixin 负责阻止新输入进入队列；这里再挡住 doAttack、doItemUse 等最终入口，
 * 是为了处理“定身同步到客户端的这一帧，点击已经被排进 MinecraftClient”的边界情况。
 * 每个入口都先清本地残留状态，再取消实际动作，保证状态结束后不会补执行。</p>
 */
@Mixin(MinecraftClient.class)
public abstract class StunnedMinecraftMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void noellesroles$clearStunnedInputAtTickStart(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (StunnedInputLock.isInputLocked(client)) {
            StunnedInputLock.releaseBlockedKeys(client);
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!StunnedInputLock.isInputLocked(client)) {
            return;
        }

        StunnedInputLock.releaseBlockedKeys(client);
        cir.setReturnValue(false);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedItemUse(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!StunnedInputLock.isInputLocked(client)) {
            return;
        }

        StunnedInputLock.releaseBlockedKeys(client);
        ci.cancel();
    }

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedItemPick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!StunnedInputLock.isInputLocked(client)) {
            return;
        }

        StunnedInputLock.releaseBlockedKeys(client);
        ci.cancel();
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedBlockBreaking(boolean breaking, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!StunnedInputLock.isInputLocked(client)) {
            return;
        }

        StunnedInputLock.releaseBlockedKeys(client);
        ci.cancel();
    }
}
