package org.agmas.noellesroles.client.mixin.roles.bounty_hunter;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 赏金模式的客户端数字键锁槽。
 *
 * <p>服务端才是权威判断；客户端这里提前取消切槽，是为了让玩家按数字键时画面不短暂闪到其他物品。</p>
 */
@Mixin(MinecraftClient.class)
public class BountyHunterLockSlotMixin {
    @WrapOperation(
            method = "handleInputEvents",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = Opcodes.PUTFIELD)
    )
    private void noellesroles$lockBountyModeSlot(@NotNull PlayerInventory inventory, int value, Operation<Void> original) {
        if (BountyHunterPlayerComponent.KEY.get(inventory.player).isBountyModeActive()
                && BountyHunterPlayerComponent.isModeGrantedDerringer(inventory.getStack(inventory.selectedSlot))
                && !BountyHunterPlayerComponent.isModeGrantedDerringer(inventory.getStack(value))) {
            return;
        }
        original.call(inventory, value);
    }
}
