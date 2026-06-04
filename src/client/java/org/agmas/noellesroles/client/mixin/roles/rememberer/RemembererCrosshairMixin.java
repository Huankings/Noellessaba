package org.agmas.noellesroles.client.mixin.roles.rememberer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.wathe.client.gui.CrosshairRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 追忆者“摸取回忆”专用准星。
 */
@Mixin(CrosshairRenderer.class)
public class RemembererCrosshairMixin {

    @Unique private static final Identifier CROSSHAIR = Identifier.of("wathe", "hud/crosshair");
    @Unique private static final Identifier CROSSHAIR_TARGET = Identifier.of("wathe", "hud/crosshair_target");
    @Unique private static final Identifier PROGRESS_BACKGROUND = Identifier.of("noellesroles", "hud/rememberer_progress_background");
    @Unique private static final Identifier PROGRESS_FILL = Identifier.of("noellesroles", "hud/rememberer_progress_fill");
    @Unique private static final Identifier READY = Identifier.of("noellesroles", "hud/rememberer_ready");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$renderRemembererCrosshair(
            @NotNull MinecraftClient client,
            @NotNull ClientPlayerEntity player,
            @NotNull DrawContext context,
            @NotNull RenderTickCounter tickCounter,
            @NotNull CallbackInfo ci
    ) {
        if (!RemembererClientEffects.shouldShowRecallCrosshair(player)) {
            return;
        }

        ci.cancel();

        boolean ready = RemembererClientEffects.canRecallNow(player);
        PlayerEntity target = RemembererClientEffects.getRecallTarget(player);
        boolean highlightTarget = target != null;

        context.getMatrices().push();
        context.getMatrices().translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F, 0.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        if (ready) {
            // 这里故意对齐 stupidexpress 小偷准星的体量与落点：
            // 图标压缩到 10x7，并放在准星正下方较近的位置，
            // 这样追忆者的交互提示不会显得过大、过低，整体观感更贴近现有职业 UI。
            context.drawGuiTexture(READY, -5, 5, 10, 7);
        } else {
            float progress = RemembererClientEffects.getRecallCooldownProgress(player, tickCounter.getTickDelta(true));
            context.drawGuiTexture(PROGRESS_BACKGROUND, -5, 5, 10, 7);
            context.drawGuiTexture(PROGRESS_FILL, 10, 7, 0, 0, -5, 5, Math.max(0, (int) (progress * 10.0F)), 7);
        }

        context.getMatrices().push();
        context.getMatrices().translate(-1.5F, -1.5F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        context.drawGuiTexture(highlightTarget ? CROSSHAIR_TARGET : CROSSHAIR, 0, 0, 3, 3);
        context.getMatrices().pop();
        context.getMatrices().pop();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
