package org.agmas.noellesroles.client.mixin.roles.insane_damned_paranoid_killer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 尸体伪装时隐藏披风。
 *
 * <p>真实 PlayerBodyEntity 不渲染玩家披风。这里只在尸体模式取消披风层，
 * 避免躺尸玩家因为披风布料动画看起来不像普通尸体。</p>
 */
@Mixin(CapeFeatureRenderer.class)
public class InsaneDamnedKillerCorpseCapeMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipCapeInInsaneDamnedKillerCorpseMode(MatrixStack matrices,
                                                                     VertexConsumerProvider vertexConsumers,
                                                                     int light,
                                                                     AbstractClientPlayerEntity player,
                                                                     float limbAngle,
                                                                     float limbDistance,
                                                                     float tickDelta,
                                                                     float animationProgress,
                                                                     float headYaw,
                                                                     float headPitch,
                                                                     CallbackInfo ci) {
        if (InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(player)) {
            ci.cancel();
        }
    }
}
