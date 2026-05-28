package org.agmas.noellesroles.client.mixin.roles.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;


@Mixin(PlayerEntityRenderer.class)
public abstract class MorphlingRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    protected MorphlingRendererMixin(EntityRendererFactory.Context context,
                                     PlayerEntityModel<AbstractClientPlayerEntity> model,
                                     float shadowRadius) {
        super(context, model, shadowRadius);
    }


    @Shadow public abstract Identifier getTexture(AbstractClientPlayerEntity abstractClientPlayerEntity);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void noellesroles$cachePlayerModels(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        // 只缓存原版的两套玩家模型，不改 renderer 本身的初始化逻辑。
        DisguiseRenderHelper.initializePlayerModels(context);
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    private void noellesroles$applyDisplayedPlayerModel(AbstractClientPlayerEntity player,
                                                        float yaw,
                                                        float tickDelta,
                                                        MatrixStack matrices,
                                                        VertexConsumerProvider vertexConsumers,
                                                        int light,
                                                        CallbackInfo ci) {
        noellesroles$applyResolvedModel(player);
    }

    @Inject(method = "renderRightArm", at = @At("HEAD"))
    private void noellesroles$applyRightArmModel(MatrixStack matrices,
                                                 VertexConsumerProvider vertexConsumers,
                                                 int light,
                                                 AbstractClientPlayerEntity player,
                                                 CallbackInfo ci) {
        // 第一人称手臂取用的是 renderer.model 里的手臂部件，
        // 所以这里也要在读取手臂前把 slim/classic 模型切到正确那一套。
        noellesroles$applyResolvedModel(player);
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"))
    private void noellesroles$applyLeftArmModel(MatrixStack matrices,
                                                VertexConsumerProvider vertexConsumers,
                                                int light,
                                                AbstractClientPlayerEntity player,
                                                CallbackInfo ci) {
        noellesroles$applyResolvedModel(player);
    }

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    void renderMorphlingSkin(AbstractClientPlayerEntity abstractClientPlayerEntity, CallbackInfoReturnable<Identifier> cir) {
        if (NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE == null) return;
        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(abstractClientPlayerEntity);
        // 检查疯狂观察者逻辑
        if (WatheClient.moodComponent != null) {
            ConfigWorldComponent configComp = ConfigWorldComponent.KEY.get(abstractClientPlayerEntity.getWorld());
            if (configComp != null && configComp.insaneSeesMorphs &&
                    WatheClient.moodComponent.isLowerThanDepressed() &&
                    NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(abstractClientPlayerEntity.getUuid())) {
                SkinTextures shuffledSkin = DisguiseRenderHelper.resolveShuffledSkinTextures(abstractClientPlayerEntity);
                if (shuffledSkin != null) {
                    cir.setReturnValue(shuffledSkin.texture());
                    cir.cancel();
                    return;
                }
            }
        }
        // 检查伪装状态
        UUID disguiseUuid = null;
        if (morphComp.getMorphTicks() > 0) {
            disguiseUuid = morphComp.disguise;
            SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(
                    abstractClientPlayerEntity,
                    disguiseUuid,
                    true
            );
            if (disguiseSkin != null) {
                // 这里直接取目标皮肤纹理，不再递归调用 getTexture，
                // 继续保留“伪装成本地玩家时也不会套娃崩客户端”的修复思路。
                cir.setReturnValue(disguiseSkin.texture());
                cir.cancel();
                return;
            }

            Log.info(LogCategory.GENERAL, "Morphling disguise not found anywhere: " + disguiseUuid);
        }
    }

    @WrapOperation(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures renderArm(AbstractClientPlayerEntity instance, Operation<SkinTextures> original) {
        if (NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE == null) return original.call(instance);

        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(instance);

        // 检查伪装状态
        if (morphComp.getMorphTicks() > 0) {
            UUID disguiseUuid = morphComp.disguise;

            if (disguiseUuid != null) {
                SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(instance, disguiseUuid, true);
                if (disguiseSkin != null) {
                    return disguiseSkin;
                }

                Log.info(LogCategory.GENERAL, "Morphling disguise not found for arm render: " + disguiseUuid);
            }
        }
        // 检查疯狂观察者逻辑
        if (WatheClient.moodComponent != null) {
            ConfigWorldComponent configComp = ConfigWorldComponent.KEY.get(instance.getWorld());
            if (configComp != null && configComp.insaneSeesMorphs &&
                    WatheClient.moodComponent.isLowerThanDepressed() &&
                    NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(instance.getUuid())) {
                SkinTextures shuffledSkin = DisguiseRenderHelper.resolveShuffledSkinTextures(instance);
                if (shuffledSkin != null) {
                    return shuffledSkin;
                }
            }
        }
        return original.call(instance);
    }

    @Unique
    private void noellesroles$applyResolvedModel(AbstractClientPlayerEntity player) {
        PlayerEntityModel<AbstractClientPlayerEntity> resolvedModel =
                DisguiseRenderHelper.getPlayerModel(DisguiseRenderHelper.resolveDisplayedSkinTextures(player));

        if (resolvedModel != null) {
            this.model = resolvedModel;
        }
    }

}
