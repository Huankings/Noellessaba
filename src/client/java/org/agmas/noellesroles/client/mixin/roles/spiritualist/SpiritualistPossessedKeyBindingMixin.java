package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 被灵术师附身的玩家，本地大部分操作都应当失效。
 */
@Mixin(value = KeyBinding.class, priority = 4995)
public abstract class SpiritualistPossessedKeyBindingMixin {

    @Unique
    private void noellesroles$lockPossessedKeys(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        if (!SpiritualistHostComponent.KEY.get(client.player).possessed) {
            return;
        }

        KeyBinding key = (KeyBinding) (Object) this;
        boolean isMoveKey = key.equals(client.options.forwardKey)
                || key.equals(client.options.backKey)
                || key.equals(client.options.leftKey)
                || key.equals(client.options.rightKey)
                || key.equals(client.options.jumpKey)
                || key.equals(client.options.sprintKey);
        boolean isActionKey = key.equals(client.options.attackKey) || key.equals(client.options.useKey);
        boolean isSneakKey = key.equals(client.options.sneakKey);
        boolean isInventoryKey = key.equals(client.options.inventoryKey);

        boolean isHotbarKey = false;
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (key.equals(hotbarKey)) {
                isHotbarKey = true;
                break;
            }
        }

        if (isMoveKey || isActionKey || isSneakKey || isInventoryKey || isHotbarKey) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "wasPressed", at = @At("RETURN"), cancellable = true)
    private void noellesroles$blockPossessedWasPressed(CallbackInfoReturnable<Boolean> cir) {
        noellesroles$lockPossessedKeys(cir);
    }

    @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
    private void noellesroles$blockPossessedIsPressed(CallbackInfoReturnable<Boolean> cir) {
        noellesroles$lockPossessedKeys(cir);
    }
}
