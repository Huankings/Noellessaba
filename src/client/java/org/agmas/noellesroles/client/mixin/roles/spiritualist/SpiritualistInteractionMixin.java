package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵魂出窍时，彻底禁止所有本地交互。
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class SpiritualistInteractionMixin {

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockProjectionAttackBlock(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (SpiritualistClientController.isProjectionActive()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockProjectionAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (SpiritualistClientController.isProjectionActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockProjectionInteractBlock(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (SpiritualistClientController.isProjectionActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockProjectionInteractItem(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (SpiritualistClientController.isProjectionActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockProjectionInteractEntity(
            PlayerEntity player,
            Entity entity,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (SpiritualistClientController.isProjectionActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockProjectionInteractEntityAt(
            PlayerEntity player,
            Entity entity,
            EntityHitResult hitResult,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (SpiritualistClientController.isProjectionActive()) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}
