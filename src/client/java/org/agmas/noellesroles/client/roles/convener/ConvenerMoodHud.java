package org.agmas.noellesroles.client.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudContext;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.util.Identifier;

/**
 * 召集者心情 HUD 样式。
 */
public final class ConvenerMoodHud {
    private static final Identifier CONVENER_MOOD = Identifier.of(NoellesRolesCore.MOD_ID, "hud/mood_convener");

    private ConvenerMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.CONVENER, MoodHudStyle
                .builder(CONVENER_MOOD)
                .bar(ConvenerMoodHud::renderGradientFlowBar)
                .build());
    }

    private static void renderGradientFlowBar(MoodHudContext context, int width, float alpha) {
        if (width <= 0 || alpha <= 0.0F) {
            return;
        }

        /*
         * Wathe 的 MoodHudApi 已经把坐标系移动到心情条原点。
         * 这里按像素列逐段填色，复刻 StupidExpress 的流动渐变条。
         */
        for (int x = 0; x < width; ++x) {
            context.drawContext().fill(x, 0, x + 1, 1, ConvenerColorHelper.getBarFlowColor(x, width, alpha));
        }
    }
}
