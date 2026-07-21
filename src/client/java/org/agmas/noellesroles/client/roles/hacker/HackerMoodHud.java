package org.agmas.noellesroles.client.roles.hacker;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 黑客心情 HUD。
 */
public final class HackerMoodHud {
    private static final Identifier HACKER_MOOD = Identifier.of(Noellesroles.MOD_ID, "hud/mood_hacker");

    private HackerMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(Noellesroles.HACKER, MoodHudStyle
                .builder(HACKER_MOOD)
                .barColor(Noellesroles.HACKER.color())
                .build());
    }
}
