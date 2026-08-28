package org.agmas.noellesroles.client.roles.lich;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.PsychoMoodHudStyle;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.lich.LichConstants;
import org.agmas.noellesroles.roles.lich.LichPsychoHandler;

/**
 * 巫妖疯魔专属心情 HUD。
 */
public final class LichMoodHud {
    private static final Identifier BODY = NoellesRolesCore.id("hud/mood_psycho_lich");
    private static final Identifier HIT_BODY = NoellesRolesCore.id("hud/mood_psycho_lich_hit");
    private static final Identifier EYES = NoellesRolesCore.id("hud/mood_psycho_lich_eyes");
    private static final PsychoMoodHudStyle STYLE = new PsychoMoodHudStyle(
            (context, psycho) -> BODY,
            (context, psycho) -> HIT_BODY,
            (context, psycho) -> EYES,
            (context, psycho) -> Text.translatable("psycho_mode.noellesroles.lich.text"),
            (context, psycho) -> LichConstants.PSYCHO_LICH_TEXT_COLOR,
            (context, psycho) -> LichConstants.PSYCHO_LICH_TIMER_BAR_COLOR
    );

    private LichMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerPsychoStyle(
                NoellesRolesCore.id("mood/lich_psycho"),
                MoodHudApi.DEFAULT_PRIORITY + LichConstants.PSYCHO_LICH_MOOD_HUD_PRIORITY_BONUS,
                (context, psycho) -> LichPsychoHandler.PROFILE_ID.equals(psycho.getProfileId()) ? STYLE : null
        );
    }
}
