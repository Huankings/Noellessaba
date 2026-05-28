package org.agmas.noellesroles.client.mixin.roles.coroner;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CapeFeatureRenderer.class)
public class CoronerCapeMixin {

    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures wrapGetSkinTextures(AbstractClientPlayerEntity player, Operation<SkinTextures> original) {
        CoronerPlayerComponent component = CoronerPlayerComponent.KEY.get(player);
        if (component.getMorphTicks() > 0 && component.disguise != null) {
            // 验尸官可以伪装成尸体，因此披风这里也必须支持“目标已不在世界实体列表里”的情况，
            // 否则玩家模型和主皮肤都变过去了，披风还露出原皮，会直接穿帮。
            SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, component.disguise, true);
            if (disguiseSkin != null) {
                return disguiseSkin;
            }
        }
        return original.call(player);

    }
}
