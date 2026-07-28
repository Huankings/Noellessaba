package org.agmas.noellesroles.client.mixin.roles.timekeeper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperRiftInputLock;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 时间狭缝期间禁用能力键和背包键。
 *
 * <p>使用 KeyBinding 层拦截，是因为 NoellesRoles 的 G 键能力和 Wathe 的 LimitedInventory
 * 都从客户端按键状态出发；在这里清掉按键队列，可以同时拦住持续按住和短按缓存。</p>
 */
@Mixin(value = KeyBinding.class, priority = 5000)
public abstract class TimekeeperRiftAbilityInventoryKeyMixin {
    @Unique
    private void noellesroles$blockRiftAbilityAndInventory(@NotNull CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        KeyBinding key = (KeyBinding) (Object) this;
        if (!TimekeeperRiftInputLock.shouldBlockKey(client, key)) {
            return;
        }

        TimekeeperRiftInputLock.releaseBlockedKeys(client);
        cir.setReturnValue(false);
    }

    @Inject(method = "wasPressed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRiftWasPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        noellesroles$blockRiftAbilityAndInventory(cir);
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockRiftIsPressed(@NotNull CallbackInfoReturnable<Boolean> cir) {
        noellesroles$blockRiftAbilityAndInventory(cir);
    }
}
