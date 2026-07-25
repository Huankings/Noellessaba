package org.agmas.noellesroles.client.roles.rememberer;

import dev.doctor4t.wathe.api.client.hud.HudOverlayApi;
import dev.doctor4t.wathe.api.client.hud.HudOverlayContext;
import dev.doctor4t.wathe.api.client.hud.HudOverlayLayer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;

/**
 * 狙击枪左键开镜遮罩与十字线。
 */
public final class RemembererSniperScopeHud {
    private static final int PRIORITY_ABOVE_STATUS_HUD = 1000;

    private RemembererSniperScopeHud() {
    }

    public static void register() {
        HudOverlayApi.register(
                NoellesHudSupport.id("roles/rememberer/sniper_scope"),
                HudOverlayLayer.AFTER_HUD,
                PRIORITY_ABOVE_STATUS_HUD,
                RemembererSniperScopeHud::render
        );
    }

    private static void render(HudOverlayContext context) {
        if (!context.aliveAndSurvival()) {
            return;
        }

        float progress = RemembererClientEffects.getSniperScopeProgress(context.tickDelta());
        if (progress <= 0.0F) {
            return;
        }

        renderScopeMask(context.drawContext(), progress);
        renderScopeCrosshair(context.drawContext());

        /*
         * 开镜遮罩画在 AFTER_HUD，已经盖住了聊天、右侧职业 HUD 等其它信息。
         * 这里只通过 Wathe 暴露的受控入口复画一次热键栏，不再 mixin InGameHud#renderHotbar。
         */
        context.renderHotbar();
    }

    /**
     * 用逐行矩形绘制“椭圆外全黑、椭圆内透明”的遮罩。
     */
    private static void renderScopeMask(DrawContext context, float progress) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        float animatedRadiusScale = MathHelper.lerp(
                progress,
                RemembererConstants.SNIPER_SCOPE_INITIAL_RADIUS_SCALE,
                1.0F
        );

        float horizontalRadius = Math.max(
                1.0F,
                width * RemembererConstants.SNIPER_SCOPE_FINAL_HORIZONTAL_RADIUS_RATIO * animatedRadiusScale
        );
        float currentAspectRatio = width / (float) Math.max(1, height);
        float verticalRadius = Math.max(
                1.0F,
                horizontalRadius * RemembererConstants.SNIPER_SCOPE_BASE_ASPECT_RATIO / currentAspectRatio
        );
        float centerX = width / 2.0F;
        float centerY = height / 2.0F;

        for (int y = 0; y < height; y++) {
            float dy = (y + 0.5F - centerY) / verticalRadius;
            if (Math.abs(dy) >= 1.0F) {
                context.fill(0, y, width, y + 1, RemembererConstants.SNIPER_SCOPE_MASK_COLOR);
                continue;
            }

            float halfVisibleWidth = horizontalRadius * MathHelper.sqrt(1.0F - dy * dy);
            int visibleLeft = MathHelper.clamp((int) Math.floor(centerX - halfVisibleWidth), 0, width);
            int visibleRight = MathHelper.clamp((int) Math.ceil(centerX + halfVisibleWidth), 0, width);
            if (visibleLeft > 0) {
                context.fill(0, y, visibleLeft, y + 1, RemembererConstants.SNIPER_SCOPE_MASK_COLOR);
            }
            if (visibleRight < width) {
                context.fill(visibleRight, y, width, y + 1, RemembererConstants.SNIPER_SCOPE_MASK_COLOR);
            }
        }
    }

    /**
     * 狙击镜中心十字线。
     */
    private static void renderScopeCrosshair(DrawContext context) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int thickness = Math.max(1, RemembererConstants.SNIPER_SCOPE_CROSSHAIR_THICKNESS);
        int centerX = width / 2;
        int centerY = height / 2;
        int halfThickness = thickness / 2;

        context.fill(
                0,
                centerY - halfThickness,
                width,
                centerY - halfThickness + thickness,
                RemembererConstants.SNIPER_SCOPE_CROSSHAIR_COLOR
        );
        context.fill(
                centerX - halfThickness,
                0,
                centerX - halfThickness + thickness,
                height,
                RemembererConstants.SNIPER_SCOPE_CROSSHAIR_COLOR
        );
    }
}
