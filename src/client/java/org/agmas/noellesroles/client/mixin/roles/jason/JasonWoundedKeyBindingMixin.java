package org.agmas.noellesroles.client.mixin.roles.jason;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityInputLock;
import org.agmas.noellesroles.client.roles.jason.JasonWoundedInputLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 倒地状态下拦截攻击、使用、跳跃、疾跑和能力键的读取。
 */
@Mixin(value = KeyBinding.class, priority = 4900)
public abstract class JasonWoundedKeyBindingMixin {
    @Unique
    private void noellesroles$blockJasonWoundedKey(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        KeyBinding key = (KeyBinding) (Object) this;
        boolean blockWounded = JasonWoundedInputLock.shouldBlockKey(client, key);
        boolean blockAbility = JasonAbilityInputLock.shouldBlockKey(client, key);
        if (!blockWounded && !blockAbility) {
            return;
        }
        if (blockWounded) {
            JasonWoundedInputLock.releaseBlockedKeys(client);
        }
        if (blockAbility) {
            JasonAbilityInputLock.releaseBlockedKeys(client);
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedWasPressed(CallbackInfoReturnable<Boolean> cir) {
        noellesroles$blockJasonWoundedKey(cir);
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedIsPressed(CallbackInfoReturnable<Boolean> cir) {
        noellesroles$blockJasonWoundedKey(cir);
    }
}
