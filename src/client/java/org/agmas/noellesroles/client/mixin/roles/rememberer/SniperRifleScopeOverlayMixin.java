package org.agmas.noellesroles.client.mixin.roles.rememberer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 狙击枪左键开镜遮罩与十字线。
 *
 * <p>遮罩必须画在 InGameHud#render 的最末尾，原因是 1.21.1 的 HUD 已经拆成 LayeredDrawer 多层渲染：
 * 如果只挂在 renderMainHud，后续的经验等级、状态效果、聊天、计分板等层仍会重新盖上来。
 * 这里在所有 HUD 画完后统一盖黑，再单独把热键栏复画一次，就能满足“只有热键栏始终可见，其它 HUD 不可见”。</p>
 */
@Mixin(InGameHud.class)
public abstract class SniperRifleScopeOverlayMixin {

    @Invoker("renderHotbar")
    protected abstract void noellesroles$renderHotbar(DrawContext context, RenderTickCounter tickCounter);

    @Inject(method = "render", at = @At("TAIL"))
    private void noellesroles$renderSniperScopeOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        float progress = RemembererClientEffects.getSniperScopeProgress(tickCounter.getTickDelta(true));
        if (progress <= 0.0F) {
            return;
        }

        noellesroles$renderScopeMask(context, progress);
        noellesroles$renderScopeCrosshair(context);

        /*
         * 按你的确认，开镜时热键栏要一直可见。
         * 因为上面的黑色遮罩已经盖住了整套 HUD，这里只复画原版热键栏本身；
         * 血量、经验条、右侧职业 HUD、聊天和计分板等其它信息不会被复画，所以仍然保持不可见。
         */
        noellesroles$renderHotbar(context, tickCounter);
    }

    /**
     * 用逐行矩形绘制“椭圆外全黑、椭圆内透明”的遮罩。
     *
     * <p>这里不使用贴图，后续只改 {@link RemembererConstants} 里的半径常量就能改变狙击镜大小。
     * 横向半径固定按屏幕宽度计算；纵向半径按 16:9 基准宽高比换算，
     * 所以 16:9 时是圆形，其它宽高比会自然变成椭圆。</p>
     */
    @Unique
    private static void noellesroles$renderScopeMask(DrawContext context, float progress) {
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
     *
     * <p>线条画满整个屏幕即可：椭圆外本来就是同色纯黑，看起来只会在可见范围内出现十字。
     * 热键栏随后会被复画，因此纵线不会压在热键栏上影响选栏辨认。</p>
     */
    @Unique
    private static void noellesroles$renderScopeCrosshair(DrawContext context) {
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
