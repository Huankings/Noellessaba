package org.agmas.noellesroles.client.mixin.roles.jason;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityInputLock;
import org.agmas.noellesroles.client.roles.jason.JasonWoundedInputLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 倒地时吞掉攻击、使用和能力键绑定到鼠标后的按键事件。
 */
@Mixin(Mouse.class)
public abstract class JasonWoundedMouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedMouse(
            long window,
            int button,
        int action,
        int modifiers,
        CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean blockWounded = JasonWoundedInputLock.shouldBlockMouseButton(client, button);
        boolean blockAbility = JasonAbilityInputLock.shouldBlockMouseButton(client, button);
        if (blockWounded || blockAbility) {
            if (blockWounded) {
                JasonWoundedInputLock.releaseBlockedKeys(client);
            }
            if (blockAbility) {
                JasonAbilityInputLock.releaseBlockedKeys(client);
            }
            ci.cancel();
        }
    }
}
