package org.agmas.noellesroles.client.mixin.roles.coroner;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.coroner.CoronerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntityRenderer.class)
public abstract class CoronerRendererMixin {

    @Shadow public abstract Identifier getTexture(AbstractClientPlayerEntity abstractClientPlayerEntity);

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"), cancellable = true)
    void renderCoronerSkin(AbstractClientPlayerEntity abstractClientPlayerEntity,
                           CallbackInfoReturnable<Identifier> cir) {
        CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(abstractClientPlayerEntity);

        // 检查伪装状态
        if (coronerComp.getMorphTicks() > 0) {
            UUID disguiseUuid = coronerComp.disguise;
            SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(
                    abstractClientPlayerEntity,
                    disguiseUuid,
                    true
            );
            if (disguiseSkin != null) {
                cir.setReturnValue(disguiseSkin.texture());
                cir.cancel();
                return;
            }

            Log.info(LogCategory.GENERAL, "Coroner disguise not found anywhere: " + disguiseUuid);
        }
    }

    @WrapOperation(method = "renderArm",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures renderArm(AbstractClientPlayerEntity instance, Operation<SkinTextures> original) {
        CoronerPlayerComponent coronerComp = CoronerPlayerComponent.KEY.get(instance);

        // 检查伪装状态
        if (coronerComp.getMorphTicks() > 0) {
            UUID disguiseUuid = coronerComp.disguise;

            if (disguiseUuid != null) {
                SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(instance, disguiseUuid, true);
                if (disguiseSkin != null) {
                    return disguiseSkin;
                }

                Log.info(LogCategory.GENERAL, "Coroner disguise not found for arm render: " + disguiseUuid);
            }
        }

        return original.call(instance);
    }
}
