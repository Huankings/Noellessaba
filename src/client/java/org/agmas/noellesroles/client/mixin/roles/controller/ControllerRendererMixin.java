package org.agmas.noellesroles.client.mixin.roles.controller;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.controller.ControllerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntityRenderer.class)
public abstract class ControllerRendererMixin {

    @Shadow public abstract Identifier getTexture(AbstractClientPlayerEntity abstractClientPlayerEntity);

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"), cancellable = true)
    void renderControllerSkin(AbstractClientPlayerEntity abstractClientPlayerEntity,
                              CallbackInfoReturnable<Identifier> cir) {
        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(abstractClientPlayerEntity);
        UUID disguiseUuid = controllerComp.getDisguiseTarget();

        // 附体师的伪装皮肤现在完全由附体师组件自己驱动，
        // 不再去读取变形怪组件的时间和目标。
        if (disguiseUuid == null) {
            return;
        }

        // 统一复用和变形怪/验尸官相同的皮肤解析链路，
        // 这样附体师如果以后也出现“目标不在世界实体里”的情况，模型和纹理仍然能一致回退。
        SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(
                abstractClientPlayerEntity,
                disguiseUuid,
                true
        );
        if (disguiseSkin != null) {
            cir.setReturnValue(disguiseSkin.texture());
            cir.cancel();
        }
    }

    @WrapOperation(method = "renderArm",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures renderControllerArm(AbstractClientPlayerEntity instance, Operation<SkinTextures> original) {
        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(instance);
        UUID disguiseUuid = controllerComp.getDisguiseTarget();

        // 这里单独处理第一人称手臂渲染，避免过去只能依赖变形怪 mixin 才能显示正确手臂皮肤。
        if (disguiseUuid == null) {
            return original.call(instance);
        }

        SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(instance, disguiseUuid, true);
        if (disguiseSkin != null) {
            return disguiseSkin;
        }

        return original.call(instance);
    }
}
