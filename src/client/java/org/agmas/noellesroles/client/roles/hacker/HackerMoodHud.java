package org.agmas.noellesroles.client.roles.hacker;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.util.Identifier;

/**
 * 黑客心情 HUD。
 */
public final class HackerMoodHud {
    private static final Identifier HACKER_MOOD = Identifier.of(NoellesRolesCore.MOD_ID, "hud/mood_hacker");

    private HackerMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.HACKER, MoodHudStyle
                .builder(HACKER_MOOD)
                .barColor(NoellesRoleRegistry.HACKER.color())
                .build());
    }
}
