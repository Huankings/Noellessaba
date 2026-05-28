package org.agmas.noellesroles.client.mixin.roles.controller;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HeldItemFeatureRenderer.class)
public class ControlledItemInvisibilityHeldMixin {

    @WrapOperation(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack hideMainHandItem(LivingEntity entity, Operation<ItemStack> original) {
        ItemStack stack = original.call(entity);
        if (entity instanceof PlayerEntity player) {
            ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(player);
            if (controlledComp.isControlled) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getOffHandStack()Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack hideOffHandItem(LivingEntity entity, Operation<ItemStack> original) {
        ItemStack stack = original.call(entity);
        if (entity instanceof PlayerEntity player) {
            ControlledPlayerComponent controlledComp = ControlledPlayerComponent.KEY.get(player);
            if (controlledComp.isControlled) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }
}