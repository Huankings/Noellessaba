package org.agmas.noellesroles.client.mixin.roles.spiritualist;

import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 灵魂出窍时启用紫色灵视滤镜，并隐藏方块高亮框。
 */
@Mixin(GameRenderer.class)
public abstract class SpiritualistVisionMixin {

    @Shadow private PostEffectProcessor postProcessor;
    @Shadow private boolean postProcessorEnabled;

    @Shadow
    protected abstract void loadPostProcessor(Identifier id);

    @Unique
    private static final Identifier SPIRITUALIST_SHADER = Identifier.ofVanilla("shaders/post/spiritualist_projection.json");

    @Unique
    private boolean spiritualistShaderActive = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void noellesroles$manageProjectionShader(CallbackInfo ci) {
        if (SpiritualistClientController.isProjectionActive() && !this.spiritualistShaderActive) {
            loadPostProcessor(SPIRITUALIST_SHADER);
            this.spiritualistShaderActive = true;
        } else if (!SpiritualistClientController.isProjectionActive() && this.spiritualistShaderActive) {
            if (this.postProcessor != null) {
                this.postProcessor.close();
            }
            this.postProcessor = null;
            this.postProcessorEnabled = false;
            this.spiritualistShaderActive = false;
        }
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void noellesroles$hideProjectionBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (SpiritualistClientController.isProjectionActive()) {
            cir.setReturnValue(false);
        }
    }
}
