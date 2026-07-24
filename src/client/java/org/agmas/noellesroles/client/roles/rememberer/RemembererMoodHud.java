package org.agmas.noellesroles.client.roles.rememberer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudContext;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.roles.rememberer.RemembererConstants;

public final class RemembererMoodHud {
    private static final Identifier REMEMBERER_ARROW_UP = Identifier.of(NoellesRolesCore.MOD_ID, "hud/rememberer_arrow_up");
    private static final Identifier REMEMBERER_ARROW_DOWN = Identifier.of(NoellesRolesCore.MOD_ID, "hud/rememberer_arrow_down");
    private static final Identifier REMEMBERER_MOOD_HAPPY = Identifier.of(NoellesRolesCore.MOD_ID, "hud/rememberer_mood_happy");
    private static final Identifier REMEMBERER_MOOD_MID = Identifier.of(NoellesRolesCore.MOD_ID, "hud/rememberer_mood_mid");
    private static final Identifier REMEMBERER_MOOD_DEPRESSIVE = Identifier.of(NoellesRolesCore.MOD_ID, "hud/rememberer_mood_depressive");

    private RemembererMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.REMEMBERER, MoodHudStyle
                .builder(RemembererMoodHud::getMoodSprite)
                .arrows(REMEMBERER_ARROW_UP, REMEMBERER_ARROW_DOWN)
                .bar(RemembererMoodHud::renderMoodBar)
                .build());
    }

    private static Identifier getMoodSprite(MoodHudContext context) {
        if (context.moodRender() < GameConstants.DEPRESSIVE_MOOD_THRESHOLD) {
            return REMEMBERER_MOOD_DEPRESSIVE;
        }
        if (context.moodRender() < GameConstants.MID_MOOD_THRESHOLD) {
            return REMEMBERER_MOOD_MID;
        }
        return REMEMBERER_MOOD_HAPPY;
    }

    private static void renderMoodBar(MoodHudContext context, int width, float alpha) {
        if (width <= 0 || alpha <= 0.0F) {
            return;
        }

        /*
         * 追忆者的心情条不是单色，而是按当前心情在三段颜色之间插值。
         * Wathe 负责把 width 算好，这里只决定“这一帧该是什么 RGB”。
         */
        int colour = getRemembererMoodBarColor(context.moodRender());
        context.drawContext().fill(0, 0, width, 1, colour | ((int) (alpha * 255.0F) << 24));
    }

    private static int getRemembererMoodBarColor(float moodRender) {
        if (moodRender >= GameConstants.MID_MOOD_THRESHOLD) {
            float delta = (moodRender - GameConstants.MID_MOOD_THRESHOLD) / (1.0F - GameConstants.MID_MOOD_THRESHOLD);
            return lerpColor(RemembererConstants.MOOD_BAR_MID_COLOR, RemembererConstants.MOOD_BAR_HAPPY_COLOR, delta);
        }
        if (moodRender >= GameConstants.DEPRESSIVE_MOOD_THRESHOLD) {
            float delta = (moodRender - GameConstants.DEPRESSIVE_MOOD_THRESHOLD)
                    / (GameConstants.MID_MOOD_THRESHOLD - GameConstants.DEPRESSIVE_MOOD_THRESHOLD);
            return lerpColor(RemembererConstants.MOOD_BAR_DEPRESSIVE_COLOR, RemembererConstants.MOOD_BAR_MID_COLOR, delta);
        }
        return RemembererConstants.MOOD_BAR_DEPRESSIVE_COLOR;
    }

    private static int lerpColor(int from, int to, float delta) {
        delta = MathHelper.clamp(delta, 0.0F, 1.0F);
        int fromR = (from >> 16) & 0xFF;
        int fromG = (from >> 8) & 0xFF;
        int fromB = from & 0xFF;
        int toR = (to >> 16) & 0xFF;
        int toG = (to >> 8) & 0xFF;
        int toB = to & 0xFF;
        int red = MathHelper.floor(MathHelper.lerp(delta, fromR, toR));
        int green = MathHelper.floor(MathHelper.lerp(delta, fromG, toG));
        int blue = MathHelper.floor(MathHelper.lerp(delta, fromB, toB));
        return (red << 16) | (green << 8) | blue;
    }
}
