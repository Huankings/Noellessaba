package org.agmas.noellesroles.client.mixin.roles.rememberer;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 狙击枪左键开镜时的 FOV 放大。
 *
 * <p>这里直接挂到 GameRenderer#getFov 的返回值上，
 * 让原版玩家设置、疾跑/药水/水下等 FOV 调整先照常算完，
 * 最后再额外乘上狙击镜自己的倍率。这样兼容性比硬改玩家 FOV 更高，
 * 也不会影响第三人称或服务端逻辑。</p>
 */
@Mixin(GameRenderer.class)
public class SniperRifleScopeFovMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void noellesroles$applySniperScopeFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        float multiplier = RemembererClientEffects.getSniperScopeFovMultiplier(tickDelta);
        if (multiplier >= 1.0F) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue() * multiplier);
    }
}
