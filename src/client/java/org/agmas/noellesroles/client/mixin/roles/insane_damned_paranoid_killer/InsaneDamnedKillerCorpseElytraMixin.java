package org.agmas.noellesroles.client.mixin.roles.insane_damned_paranoid_killer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 尸体伪装时隐藏鞘翅层。
 *
 * <p>这和披风处理一样只属于客户端视觉收口；目标隐藏、碰撞和速度都已经走 Wathe 公开 API。</p>
 */
@Mixin(ElytraFeatureRenderer.class)
public class InsaneDamnedKillerCorpseElytraMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void noellesroles$skipElytraInInsaneDamnedKillerCorpseMode(MatrixStack matrices,
                                                                       VertexConsumerProvider vertexConsumers,
                                                                       int light,
                                                                       LivingEntity entity,
                                                                       float limbAngle,
                                                                       float limbDistance,
                                                                       float tickDelta,
                                                                       float animationProgress,
                                                                       float headYaw,
                                                                       float headPitch,
                                                                       CallbackInfo ci) {
        /*
         * ElytraFeatureRenderer 是 LivingEntity 泛型渲染层，运行时方法描述符会擦成 LivingEntity。
         * 这里必须按真实描述符接参，再收窄到客户端玩家；否则 Mixin 会在启动时因描述符不一致直接失败。
         */
        if (entity instanceof AbstractClientPlayerEntity player
                && InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(player)) {
            ci.cancel();
        }
    }
}
