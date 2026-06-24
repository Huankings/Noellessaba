package org.agmas.noellesroles.client.mixin;

import dev.doctor4t.wathe.client.render.entity.PlayerBodyEntityRenderer;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.renderer.PlayerBodySkinRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修正 Wathe 尸体渲染的皮肤来源。
 *
 * <p>Wathe 原版尸体只保存 owner UUID，渲染时每帧从 PLAYER_ENTRIES_CACHE 取皮肤。
 * noellesroles 的变形系统又会在玩家渲染入口改写“当前显示皮肤”，于是尸体很容易读到
 * 客户端某个阶段的临时外观，表现为变形者死亡后尸体仍然像伪装目标，或者缓存刷新后才恢复。</p>
 *
 * <p>这里把尸体渲染单独收口到 PlayerBodySkinRenderHelper：
 * 1. 正常情况下只显示尸体 owner 的原始皮肤；
 * 2. 灵术师灵魂出窍时，本地看到的所有尸体都显示成本地灵术师自己的原始皮肤；
 * 3. 贴图和 slim/classic 模型都使用同一份 SkinTextures，避免贴图和手臂模型不一致。</p>
 */
@Mixin(PlayerBodyEntityRenderer.class)
public abstract class PlayerBodySkinRendererMixin extends LivingEntityRenderer<PlayerBodyEntity, PlayerEntityModel<PlayerBodyEntity>> {
    protected PlayerBodySkinRendererMixin(EntityRendererFactory.Context context,
                                          PlayerEntityModel<PlayerBodyEntity> entityModel,
                                          float shadowRadius) {
        super(context, entityModel, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void noellesroles$cacheBodyModels(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        PlayerBodySkinRenderHelper.initializeBodyModels(context);
    }

    @Inject(
            method = "render(Ldev/doctor4t/wathe/entity/PlayerBodyEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD")
    )
    private void noellesroles$applyResolvedBodyModel(
            PlayerBodyEntity body,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        SkinTextures skinTextures = PlayerBodySkinRenderHelper.resolveBodySkinTextures(body);
        PlayerEntityModel<PlayerBodyEntity> resolvedModel = PlayerBodySkinRenderHelper.getBodyModel(skinTextures);
        if (resolvedModel != null) {
            this.model = resolvedModel;
        }
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void noellesroles$useStableBodyTexture(
            PlayerBodyEntity body,
            CallbackInfoReturnable<Identifier> cir
    ) {
        cir.setReturnValue(PlayerBodySkinRenderHelper.resolveBodyTexture(
                body,
                PlayerBodyEntityRenderer.DEFAULT_TEXTURE
        ));
    }
}
