package org.agmas.noellesroles.client.mixin.roles.jason;

import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityInputLock;
import org.agmas.noellesroles.client.roles.jason.JasonWoundedInputLock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 倒地状态的最终客户端交互兜底。
 *
 * <p>即便客户端同步到倒地状态时左/右键已经排进本帧，也必须在 MinecraftClient 实际执行
 * 攻击、使用或破坏方块前取消，服务端另有同样的攻击/使用限制作为权限边界。</p>
 */
@Mixin(MinecraftClient.class)
public abstract class JasonWoundedMinecraftMixin {
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void noellesroles$clearJasonWoundedInput(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean wounded = JasonWoundedInputLock.isInputLocked(client);
        boolean ability = JasonAbilityInputLock.isInputLocked(client);
        if (wounded || ability) {
            if (wounded) {
                JasonWoundedInputLock.releaseBlockedKeys(client);
            }
            if (ability) {
                JasonAbilityInputLock.releaseBlockedKeys(client);
            }
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean wounded = JasonWoundedInputLock.isInputLocked(client);
        boolean ability = JasonAbilityInputLock.isInputLocked(client);
        if (wounded || ability) {
            if (wounded) {
                JasonWoundedInputLock.releaseBlockedKeys(client);
            }
            if (ability) {
                JasonAbilityInputLock.releaseBlockedKeys(client);
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedUse(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean wounded = JasonWoundedInputLock.isInputLocked(client);
        boolean ability = JasonAbilityInputLock.isInputLocked(client);
        if (wounded || ability) {
            if (wounded) {
                JasonWoundedInputLock.releaseBlockedKeys(client);
            }
            if (ability) {
                JasonAbilityInputLock.releaseBlockedKeys(client);
            }
            ci.cancel();
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockJasonWoundedBlockBreaking(boolean breaking, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean wounded = JasonWoundedInputLock.isInputLocked(client);
        boolean ability = JasonAbilityInputLock.isInputLocked(client);
        if (wounded || ability) {
            if (wounded) {
                JasonWoundedInputLock.releaseBlockedKeys(client);
            }
            if (ability) {
                JasonAbilityInputLock.releaseBlockedKeys(client);
            }
            ci.cancel();
        }
    }
}
