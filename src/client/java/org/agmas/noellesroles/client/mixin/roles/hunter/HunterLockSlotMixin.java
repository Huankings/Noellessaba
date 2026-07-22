package org.agmas.noellesroles.client.mixin.roles.hunter;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.hunter.HunterPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 客户端输入层的追猎者猎刀锁槽。
 */
@Mixin(MinecraftClient.class)
public class HunterLockSlotMixin {

    @WrapOperation(method = "handleInputEvents", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = Opcodes.PUTFIELD))
    private void noellesroles$lockHunterSlot(@NotNull PlayerInventory inventory, int value, Operation<Void> original) {
        if (MinecraftClient.getInstance().player == null) {
            return;
        }

        int lockSlot = inventory.selectedSlot;
        HunterPlayerComponent hunter = HunterPlayerComponent.KEY.get(MinecraftClient.getInstance().player);
        if (inventory.player.isUsingItem() && GameFunctions.isPlayerAliveAndSurvival(inventory.player) && hunter.isSprinting) {
            ItemStack activeStack = inventory.player.getActiveItem();
            if (activeStack.isOf(ModItems.HUNTING_KNIFE)
                    && activeStack.getItem().getUseAction(activeStack) == UseAction.SPEAR
                    && inventory.getStack(lockSlot).isOf(ModItems.HUNTING_KNIFE)
                    && !inventory.getStack(value).isOf(ModItems.HUNTING_KNIFE)) {
                return;
            }
        }

        original.call(inventory, value);
    }
}
