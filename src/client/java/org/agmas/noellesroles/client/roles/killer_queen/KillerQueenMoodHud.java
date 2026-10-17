package org.agmas.noellesroles.client.roles.killer_queen;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import dev.doctor4t.wathe.api.client.mood.PsychoMoodHudStyle;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenConstants;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenPsychoHandler;

/** 杀手皇后普通与疯魔 Mood HUD。 */
public final class KillerQueenMoodHud {
    private static final Identifier NORMAL = NoellesRolesCore.id("hud/mood_killer_queen");
    private static final Identifier BODY = NoellesRolesCore.id("hud/mood_psycho_killer_queen");
    private static final Identifier HIT_BODY = NoellesRolesCore.id("hud/mood_psycho_killer_queen_hit");
    private static final Identifier EYES = NoellesRolesCore.id("hud/mood_psycho_killer_queen_eyes");
    private static final PsychoMoodHudStyle PSYCHO_STYLE = new PsychoMoodHudStyle(
            (context, psycho) -> BODY,
            (context, psycho) -> HIT_BODY,
            (context, psycho) -> EYES,
            (context, psycho) -> Text.translatable("psycho_mode.noellesroles.killer_queen.text"),
            (context, psycho) -> KillerQueenConstants.ROLE_COLOR,
            (context, psycho) -> KillerQueenConstants.ROLE_COLOR
    );

    private KillerQueenMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.KILLER_QUEEN,
                MoodHudStyle.builder(NORMAL).barColor(KillerQueenConstants.ROLE_COLOR).build());
        MoodHudApi.registerPsychoStyle(NoellesRolesCore.id("mood/killer_queen_psycho"),
                MoodHudApi.DEFAULT_PRIORITY + KillerQueenConstants.PSYCHO_MOOD_HUD_PRIORITY_BONUS,
                (context, psycho) -> KillerQueenPsychoHandler.PROFILE_ID.equals(psycho.getProfileId()) ? PSYCHO_STYLE : null);
    }
}
