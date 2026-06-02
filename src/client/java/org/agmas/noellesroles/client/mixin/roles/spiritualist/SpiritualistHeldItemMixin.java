package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualProjectionCamera;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修正灵术师特殊视角下的第一人称手持渲染。
 */
@Mixin(HeldItemRenderer.class)
public abstract class SpiritualistHeldItemMixin {

    @Unique
    private float noellesroles$tickDelta;

    @ModifyVariable(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ClientPlayerEntity noellesroles$useProjectionCameraForArms(ClientPlayerEntity player) {
        if (SpiritualistClientController.isProjectionActive()
                && SpiritualistClientController.getProjectionCamera() != null) {
            return SpiritualistClientController.getProjectionCamera();
        }
        if (SpiritualistClientController.isPossessionViewActive()
                && SpiritualistClientController.getPossessionCamera() != null) {
            return SpiritualistClientController.getPossessionCamera();
        }
        return player;
    }

    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD")
    )
    private void noellesroles$storeTickDelta(
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player,
            int light,
            CallbackInfo ci
    ) {
        this.noellesroles$tickDelta = tickDelta;
    }

    @ModifyVariable(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private int noellesroles$useDetachedCameraLight(int light) {
        if (!SpiritualistClientController.isProjectionActive() && !SpiritualistClientController.isPossessionViewActive()) {
            return light;
        }

        Entity cameraEntity = MinecraftClient.getInstance().getCameraEntity();
        if (cameraEntity == null) {
            return light;
        }

        return MinecraftClient.getInstance().getEntityRenderDispatcher().getLight(cameraEntity, this.noellesroles$tickDelta);
    }
}
