package org.agmas.noellesroles.client.roles.rememberer;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/**
 * 狙击枪专用准星。
 *
 * <p>这里只在“当前没有被方块遮挡且物品不在冷却”时做目标高亮，
 * 开火本身依旧走服务端那条可穿墙弹道，不会因为客户端准星限制而失去穿墙击杀能力。</p>
 */
public final class RemembererSniperCrosshair {
    private RemembererSniperCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/rememberer/sniper_rifle"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                RemembererSniperCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!RemembererClientEffects.shouldRenderSniperCrosshair(context.player())) {
            return CrosshairHudApi.Result.PASS;
        }

        /*
         * 开镜动画期间只保留狙击镜自己的黑色十字线。
         * 这里仍然吞掉 Wathe 原准心，但不再绘制狙击枪原来的 3x3 准心，
         * 避免画面中心同时出现两套瞄准标记。
         */
        if (RemembererClientEffects.isSniperScopeVisible()) {
            return CrosshairHudApi.Result.HANDLED;
        }

        boolean target = !context.player().getItemCooldownManager().isCoolingDown(ModItems.SNIPER_RIFLE)
                && RemembererClientEffects.hasVisibleSniperTarget(context.player());
        CrosshairHudApi.renderStandardCrosshair(context, target);
        return CrosshairHudApi.Result.HANDLED;
    }
}
