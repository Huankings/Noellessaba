package org.agmas.noellesroles.client.mixin.roles.controller;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KeyBinding.class, priority = 5000)
public abstract class ControlledKeyBindingMixin {

    @Unique
    private void controlledLockKeys(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(client.player);
        if (controlledComp.isControlled) {
            KeyBinding key = (KeyBinding) (Object) this;

            // 移动键
            boolean isMoveKey = key.equals(client.options.forwardKey) ||
                    key.equals(client.options.backKey) ||
                    key.equals(client.options.leftKey) ||
                    key.equals(client.options.rightKey);
            // 动作键
            boolean isActionKey = key.equals(client.options.attackKey) ||
                    key.equals(client.options.useKey);
            // 聊天键
            boolean isChatKey = key.equals(client.options.chatKey);
            // 潜行键
            boolean isSneakKey = key.equals(client.options.sneakKey);
            // 物品栏键（E）
            boolean isInventoryKey = key.equals(client.options.inventoryKey);
            // 数字键 1-9（热键栏）
            boolean isHotbarKey = false;
            for (int i = 0; i < client.options.hotbarKeys.length; i++) {
                if (key.equals(client.options.hotbarKeys[i])) {
                    isHotbarKey = true;
                    break;
                }
            }

            if (isMoveKey || isActionKey || isChatKey || isSneakKey || isInventoryKey || isHotbarKey) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "wasPressed", at = @At("RETURN"), cancellable = true)
    private void wasPressed(CallbackInfoReturnable<Boolean> cir) {
        controlledLockKeys(cir);
    }

    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void isPressed(CallbackInfoReturnable<Boolean> cir) {
        controlledLockKeys(cir);
    }
}