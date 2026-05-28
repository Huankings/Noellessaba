package org.agmas.noellesroles.client.mixin.roles.engineer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntityRenderer.class)
public class EngineerHandMixin {
    @WrapOperation(method = "getArmPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;"))
    private static ItemStack hideCaptureDeviceForArmPose(AbstractClientPlayerEntity player, Hand hand, Operation<ItemStack> original) {
        ItemStack stack = original.call(player, hand);
        if (stack.isOf(ModItems.CAPTURE_DEVICE) && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, Noellesroles.ENGINEER)) {
            return ItemStack.EMPTY;
        }
        return stack;
    }
}