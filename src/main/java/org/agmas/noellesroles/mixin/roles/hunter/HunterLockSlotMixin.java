package org.agmas.noellesroles.mixin.roles.hunter;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.hunter.HunterPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 追猎者举猎刀疾跑时禁止滚轮切走当前格子。
 */
@Mixin(PlayerInventory.class)
public class HunterLockSlotMixin {

    @Shadow @Final @NotNull public PlayerEntity player;

    @WrapMethod(method = "scrollInHotbar")
    private void noellesroles$lockHunterSlot(double scrollAmount, @NotNull Operation<Void> original) {
        int lockSlot = this.player.getInventory().selectedSlot;
        original.call(scrollAmount);

        HunterPlayerComponent hunter = HunterPlayerComponent.KEY.get(this.player);
        if (!this.player.isUsingItem() || !GameFunctions.isPlayerAliveAndSurvival(this.player) || !hunter.isSprinting) {
            return;
        }

        ItemStack activeStack = this.player.getActiveItem();
        if (activeStack.isOf(ModItems.HUNTING_KNIFE)
                && activeStack.getItem().getUseAction(activeStack) == UseAction.SPEAR
                && this.player.getInventory().getStack(lockSlot).isOf(ModItems.HUNTING_KNIFE)
                && !this.player.getInventory().getStack(this.player.getInventory().selectedSlot).isOf(ModItems.HUNTING_KNIFE)) {
            this.player.getInventory().selectedSlot = lockSlot;
        }
    }
}
