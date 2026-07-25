package org.agmas.noellesroles.mixin.roles.bounty_hunter;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 赏金模式期间禁止滚轮切走模式德林加。
 */
@Mixin(PlayerInventory.class)
public class BountyHunterInventoryLockMixin {
    @Shadow
    @Final
    public PlayerEntity player;

    @WrapMethod(method = "scrollInHotbar")
    private void noellesroles$lockBountyModeScroll(double scrollAmount, @NotNull Operation<Void> original) {
        int oldSlot = this.player.getInventory().selectedSlot;
        original.call(scrollAmount);

        if (BountyHunterPlayerComponent.KEY.get(this.player).isBountyModeActive()
                && BountyHunterPlayerComponent.isModeGrantedDerringer(this.player.getInventory().getStack(oldSlot))
                && !BountyHunterPlayerComponent.isModeGrantedDerringer(this.player.getInventory().getStack(this.player.getInventory().selectedSlot))) {
            this.player.getInventory().selectedSlot = oldSlot;
        }
    }
}
