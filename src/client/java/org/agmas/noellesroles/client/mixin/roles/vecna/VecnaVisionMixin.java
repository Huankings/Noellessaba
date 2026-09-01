package org.agmas.noellesroles.client.mixin.roles.vecna;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.vecna.VecnaConstants;
import org.agmas.noellesroles.roles.vecna.VecnaPlayerComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 颠倒玩家的反转色视角。
 *
 * <p>该效果采用和灵术师末影人式灵视相同的 GameRenderer 后处理器路径：
 * shader 在整帧已经完成后统一交换 RGB 通道并保留亮度，
 * 因而不会只改变方块或实体而漏掉 HUD/粒子。颠倒状态结束时立即关闭处理器。</p>
 */
@Mixin(GameRenderer.class)
public abstract class VecnaVisionMixin {
    @Shadow private PostEffectProcessor postProcessor;
    @Shadow private boolean postProcessorEnabled;
    @Shadow protected abstract void loadPostProcessor(Identifier id);

    @Unique private static final Identifier NOELLESROLES_VECNA_SHADER =
            Identifier.ofVanilla("shaders/post/vecna_inversion.json");
    @Unique private boolean noellesroles$vecnaShaderActive;

    @Inject(method = "render", at = @At("HEAD"))
    private void noellesroles$manageVecnaShader(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean active = VecnaConstants.ENABLE_REVERSED_VIEW
                && client.player != null
                && GameFunctions.isPlayerAliveAndSurvival(client.player)
                && VecnaPlayerComponent.KEY.get(client.player).isPsychoInverted();
        if (active && !noellesroles$vecnaShaderActive) {
            loadPostProcessor(NOELLESROLES_VECNA_SHADER);
            noellesroles$vecnaShaderActive = true;
        } else if (!active && noellesroles$vecnaShaderActive) {
            if (postProcessor != null) postProcessor.close();
            postProcessor = null;
            postProcessorEnabled = false;
            noellesroles$vecnaShaderActive = false;
        }
    }
}
