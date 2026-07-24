package org.agmas.noellesroles.client.roles.starstruck;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudContext;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.roles.starstruck.StarstruckPlayerComponent;

import java.util.List;

/**
 * 星界使者心情 HUD。
 */
public final class StarstruckMoodHud {
    private static final Identifier ABILITY_HAPPY = id("ability_happy");
    private static final Identifier ABILITY_MID = id("ability_mid");
    private static final Identifier ABILITY_DEPRESSIVE = id("ability_depressive");
    private static final Identifier ABILITY_SPARKLES = id("ability_sparkles");
    private static final Identifier HAPPY = id("happy");
    private static final Identifier MID = id("mid");
    private static final Identifier DEPRESSIVE = id("depressive");
    private static final Identifier SPARKLES = id("sparkles");

    private StarstruckMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.STARSTRUCK, MoodHudStyle
                .builder(StarstruckMoodHud::getMoodSprite)
                .arrows()
                .overlays(StarstruckMoodHud::getOverlays)
                .bar(StarstruckMoodHud::renderMoodBar)
                .build());
    }

    private static Identifier id(String path) {
        return Identifier.of(NoellesRolesCore.MOD_ID, "hud/starstruck/" + path);
    }

    private static Identifier getMoodSprite(MoodHudContext context) {
        boolean abilityActive = StarstruckPlayerComponent.KEY.get(context.player()).ticks > 0;
        if (context.moodRender() < GameConstants.DEPRESSIVE_MOOD_THRESHOLD) {
            return abilityActive ? ABILITY_DEPRESSIVE : DEPRESSIVE;
        }
        if (context.moodRender() < GameConstants.MID_MOOD_THRESHOLD) {
            return abilityActive ? ABILITY_MID : MID;
        }
        return abilityActive ? ABILITY_HAPPY : HAPPY;
    }

    private static List<Identifier> getOverlays(MoodHudContext context) {
        if (StarstruckPlayerComponent.KEY.get(context.player()).ticks > 0) {
            return List.of(ABILITY_SPARKLES);
        }
        if (AbilityPlayerComponent.KEY.get(context.player()).cooldown == 0) {
            return List.of(SPARKLES);
        }
        return List.of();
    }

    private static void renderMoodBar(MoodHudContext context, int width, float alpha) {
        if (width <= 0 || alpha <= 0.0F) {
            return;
        }

        /*
         * 星界使者保留 StarryExpress 的紫色渐变。
         * 当前心情越高越接近亮紫，低心情时则压到更暗的蓝紫。
         */
        int colour = lerpColor(0x271BAD, 0x6156E6, context.moodRender());
        context.drawContext().fill(0, 0, width, 1, colour | ((int) (alpha * 255.0F) << 24));
    }

    private static int lerpColor(int from, int to, float delta) {
        delta = MathHelper.clamp(delta, 0.0F, 1.0F);
        int red = MathHelper.floor(MathHelper.lerp(delta, (from >> 16) & 0xFF, (to >> 16) & 0xFF));
        int green = MathHelper.floor(MathHelper.lerp(delta, (from >> 8) & 0xFF, (to >> 8) & 0xFF));
        int blue = MathHelper.floor(MathHelper.lerp(delta, from & 0xFF, to & 0xFF));
        return (red << 16) | (green << 8) | blue;
    }
}
