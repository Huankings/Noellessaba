package org.agmas.noellesroles.client.mixin.roles.controller;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntityRenderer.class)
public class ControlledItemInvisibilityArmPoseMixin {

    @WrapOperation(
            method = "getArmPose",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;")
    )
    private static ItemStack hideItemForArmPose(AbstractClientPlayerEntity player, Hand hand, Operation<ItemStack> original) {
        ItemStack stack = original.call(player, hand);
        ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(player);
        if (controlledComp.isControlled) {
            return ItemStack.EMPTY;
        }
        return stack;
    }
}