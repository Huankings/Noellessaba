package org.agmas.noellesroles.client.mixin.roles.insane_damned_paranoid_killer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 亡语杀手尸体伪装的玩家模型躺倒变换。
 *
 * <p>Wathe 当前公开 API 能处理外观、准心和碰撞，但没有“把活玩家渲染成尸体姿态”的入口。
 * 因此这里保留一条极窄的客户端渲染 mixin，只在组件明确处于尸体模式时替换玩家 transform。</p>
 */
@Mixin(PlayerEntityRenderer.class)
public class InsaneDamnedKillerCorpsePoseMixin {
    @Inject(
            method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void noellesroles$applyInsaneDamnedKillerCorpsePose(AbstractClientPlayerEntity player,
                                                                MatrixStack matrices,
                                                                float animationProgress,
                                                                float bodyYaw,
                                                                float tickDelta,
                                                                float scale,
                                                                CallbackInfo ci) {
        if (!InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(player)) {
            return;
        }

        /*
         * 这组变换搬自 spark 版 Morphling，并参考 Wathe PlayerBodyEntityRenderer 的躺倒方向。
         * 只取消原版 setupTransforms，不取消后续皮肤 / 装备层渲染，让玩家本体仍按真实皮肤显示为“地上的尸体”。
         */
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F - bodyYaw));
        matrices.translate(1.0F, 0.0F, 0.0F);
        matrices.translate(0.0F, 0.15F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        ci.cancel();
    }
}
