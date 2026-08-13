package org.agmas.noellesroles.client.mixin.roles.engineer;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.roles.engineer.StunnedInputLock;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 眩晕 / 定身期间拦截离散键盘操作。
 *
 * <p>KeyBinding 层可以阻止游戏逻辑读取按键，但 Keyboard#onKey 仍可能先把按键事件
 * 写入 pressed / timesPressed。这里在事件入口提前吞掉攻击、使用、背包、选槽等操作的
 * PRESS / REPEAT 事件，并主动清空对应队列，避免定身结束后出现最后一帧竞态导致的补执行。</p>
 *
 * <p>RELEASE 事件必须放行：如果把释放事件也取消，原版按键可能会保持 pressed=true，
 * 玩家松手后仍被当成持续按住。释放事件放行前会先清理一次残留计数。</p>
 */
@Mixin(Keyboard.class)
public abstract class StunnedKeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockStunnedKeyboardInput(
            long window,
            int key,
            int scancode,
            int action,
            int modifiers,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!StunnedInputLock.shouldBlockKeyboardPress(client, key, scancode)) {
            return;
        }

        StunnedInputLock.releaseBlockedKeys(client);
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            ci.cancel();
        }
    }
}
