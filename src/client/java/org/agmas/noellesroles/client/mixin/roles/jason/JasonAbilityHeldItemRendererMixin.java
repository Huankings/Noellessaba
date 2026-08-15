package org.agmas.noellesroles.client.mixin.roles.jason;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityArmAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 杰森无恶不在时的第一人称手臂收放动画。
 *
 * <p>这里直接包住 HeldItemRenderer 的整段 first-person 渲染：
 * 空手时会把手臂慢慢收出屏幕，持物时会把物品和手臂一起收走；
 * 退出无恶不在时则完全反向恢复。这样动画时长就会和进入 / 退出过渡时间一致。</p>
 */
@Mixin(HeldItemRenderer.class)
public abstract class JasonAbilityHeldItemRendererMixin {
    @Unique
    private boolean noellesroles$appliedJasonArmTransform;

    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD")
    )
    private void noellesroles$pushJasonArmTransform(
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player,
            int light,
            CallbackInfo ci
    ) {
        this.noellesroles$appliedJasonArmTransform = false;
        if (!JasonAbilityArmAnimator.shouldAnimate(player)) {
            return;
        }

        float visibility = JasonAbilityArmAnimator.getArmVisibility(player, tickDelta);
        matrices.push();
        JasonAbilityArmAnimator.applyFirstPersonTransform(matrices, visibility);
        this.noellesroles$appliedJasonArmTransform = true;
    }

    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("RETURN")
    )
    private void noellesroles$popJasonArmTransform(
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player,
            int light,
            CallbackInfo ci
    ) {
        if (!this.noellesroles$appliedJasonArmTransform) {
            return;
        }
        matrices.pop();
        this.noellesroles$appliedJasonArmTransform = false;
    }
}
