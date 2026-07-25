package org.agmas.noellesroles.client.roles.licensed_villain;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 执照恶棍心情 HUD 样式。
 */
public final class LicensedVillainMoodHud {
    private static final Identifier LICENSED_VILLAIN_MOOD = Identifier.of(NoellesRolesCore.MOD_ID, "hud/mood_licensed_villain");

    private LicensedVillainMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.LICENSED_VILLAIN, MoodHudStyle
                .builder(LICENSED_VILLAIN_MOOD)
                .barColor(NoellesRoleRegistry.LICENSED_VILLAIN.color())
                .build());
    }
}
