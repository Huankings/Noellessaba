package org.agmas.noellesroles.client.mixin.roles.controller;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(CapeFeatureRenderer.class)
public class ControllerCapeMixin {

    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures wrapGetSkinTextures(AbstractClientPlayerEntity player, Operation<SkinTextures> original) {
        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(player);
        UUID disguiseUuid = controllerComp.getDisguiseTarget();

        // 披风纹理也要跟随附体师自己的伪装状态走，
        // 否则切断对变形怪组件的依赖后，披风会恢复成附体师原皮。
        if (disguiseUuid == null) {
            return original.call(player);
        }

        SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, disguiseUuid, true);
        if (disguiseSkin != null) {
            return disguiseSkin;
        }

        return original.call(player);
    }
}
