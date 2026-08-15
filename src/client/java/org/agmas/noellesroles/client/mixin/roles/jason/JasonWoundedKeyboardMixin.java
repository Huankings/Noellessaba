package org.agmas.noellesroles.client.mixin.roles.jason;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityInputLock;
import org.agmas.noellesroles.client.roles.jason.JasonWoundedInputLock;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在原版把按键写入 KeyBinding 队列前，先截断倒地玩家的禁用操作。
 */
@Mixin(Keyboard.class)
public abstract class JasonWoundedKeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedKeyboard(
            long window,
            int key,
            int scancode,
            int action,
            int modifiers,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean blockWounded = JasonWoundedInputLock.shouldBlockKeyboardPress(client, key, scancode);
        boolean blockAbility = JasonAbilityInputLock.shouldBlockKeyboardPress(client, key, scancode);
        if (!blockWounded && !blockAbility) {
            return;
        }

        if (blockWounded) {
            JasonWoundedInputLock.releaseBlockedKeys(client);
        }
        if (blockAbility) {
            JasonAbilityInputLock.releaseBlockedKeys(client);
        }
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            ci.cancel();
        }
    }
}
