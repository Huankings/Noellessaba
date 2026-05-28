package org.agmas.noellesroles.client.mixin.roles.morphling;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;
import org.agmas.noellesroles.roles.morphling.MorphlingPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(CapeFeatureRenderer.class)
public abstract class MorphlingCapeMixin {

    @WrapOperation(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSkinTextures()Lnet/minecraft/client/util/SkinTextures;"))
    private SkinTextures wrapGetSkinTextures(AbstractClientPlayerEntity player, Operation<SkinTextures> original) {
        // 疯狂观察者逻辑（优先于伪装）
        if (WatheClient.moodComponent != null) {
            ConfigWorldComponent configComp = ConfigWorldComponent.KEY.get(player.getWorld());
            if (configComp != null && configComp.insaneSeesMorphs &&
                    WatheClient.moodComponent.isLowerThanDepressed() &&
                    NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE != null &&
                    NoellesrolesClient.SHUFFLED_PLAYER_ENTRIES_CACHE.containsKey(player.getUuid())) {
                SkinTextures shuffledSkin = DisguiseRenderHelper.resolveShuffledSkinTextures(player);
                if (shuffledSkin != null) {
                    return shuffledSkin;
                }
            }
        }

        // 伪装逻辑
        MorphlingPlayerComponent morphComp = MorphlingPlayerComponent.KEY.get(player);
        if (morphComp.getMorphTicks() > 0) {
            UUID disguiseUuid = morphComp.disguise;
            if (disguiseUuid != null) {
                SkinTextures disguiseSkin = DisguiseRenderHelper.resolveSkinTexturesFromUuid(player, disguiseUuid, true);
                if (disguiseSkin != null) {
                    return disguiseSkin;
                }
            }
        }

        return original.call(player);
    }
}
