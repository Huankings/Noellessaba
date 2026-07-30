package org.agmas.noellesroles.client.roles.rememberer;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/**
 * 追忆者“摸取回忆”专用准星。
 */
public final class RemembererRecallCrosshair {
    private static final Identifier PROGRESS_BACKGROUND = NoellesRolesCore.id("hud/rememberer_progress_background");
    private static final Identifier PROGRESS_FILL = NoellesRolesCore.id("hud/rememberer_progress_fill");
    private static final Identifier READY = NoellesRolesCore.id("hud/rememberer_ready");

    private RemembererRecallCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/rememberer/recall"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                RemembererRecallCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!RemembererClientEffects.shouldShowRecallCrosshair(context.player())) {
            return CrosshairHudApi.Result.PASS;
        }

        boolean ready = RemembererClientEffects.canRecallNow(context.player());
        PlayerEntity target = RemembererClientEffects.getRecallTarget(context.player());
        boolean highlightTarget = target != null;
        float progress = RemembererClientEffects.getRecallCooldownProgress(context.player(), context.tickDelta());
        /*
         * 这里故意对齐 stupidexpress 小偷准星的体量与落点：
         * 图标压缩到 10x7，并放在准星正下方较近的位置，
         * 这样追忆者的交互提示不会显得过大、过低，整体观感更贴近现有职业 UI。
         */
        CrosshairHudApi.renderIconProgressCrosshair(context, highlightTarget, ready, progress, READY, PROGRESS_BACKGROUND, PROGRESS_FILL);
        return CrosshairHudApi.Result.HANDLED;
    }
}
