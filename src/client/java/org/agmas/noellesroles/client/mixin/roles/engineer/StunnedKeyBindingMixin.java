package org.agmas.noellesroles.client.mixin.roles.engineer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.roles.engineer.StunnedInputLock;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 眩晕 / 定身期间拦截按键读取，并主动清掉 KeyBinding 内部残留。
 *
 * <p>不能只把返回值改成 false：原版 KeyBinding#wasPressed 会依赖 timesPressed 计数，
 * 如果定身期间玩家按了 E、左右键或数字键，这些计数会在状态结束后继续被消费。
 * 因此每次命中眩晕锁时都调用 {@link StunnedInputLock#releaseBlockedKeys(MinecraftClient)}
 * 同步清 pressed 与 timesPressed。</p>
 */
@Mixin(value = KeyBinding.class, priority = 5000)
public abstract class StunnedKeyBindingMixin {
    @Unique
    private void noellesroles$blockStunnedKey(@NotNull CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        KeyBinding key = (KeyBinding) (Object) this;
        if (!StunnedInputLock.shouldBlockKey(client, key)) {
            return;
        }

        StunnedInputLock.releaseBlockedKeys(client);
        cir.setReturnValue(false);
    }

    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedWasPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        noellesroles$blockStunnedKey(cir);
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedIsPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        noellesroles$blockStunnedKey(cir);
    }
}
