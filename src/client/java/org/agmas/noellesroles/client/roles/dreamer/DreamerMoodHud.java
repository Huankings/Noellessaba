package org.agmas.noellesroles.client.roles.dreamer;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudContext;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.Noellesroles;

/**
 * 梦者心情 HUD。
 */
public final class DreamerMoodHud {
    private static final Identifier DREAMER_MOOD = Identifier.of(Noellesroles.MOD_ID, "hud/mood_dreamer");

    private DreamerMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(Noellesroles.DREAMER, MoodHudStyle
                .builder(DREAMER_MOOD)
                .bar(DreamerMoodHud::renderRainbowBar)
                .build());
    }

    private static void renderRainbowBar(MoodHudContext context, int width, float alpha) {
        if (width <= 0 || alpha <= 0.0F) {
            return;
        }

        float rainbowTime = (System.currentTimeMillis() % 6000) / 6000.0F;
        int rainbowColor = MathHelper.hsvToRgb(rainbowTime, 1.0F, 1.0F);
        context.drawContext().fill(0, 0, width, 1, rainbowColor | ((int) (alpha * 255.0F) << 24));
    }
}
