package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.PsychoMoodHudStyle;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.agmas.noellesroles.roles.jason.JasonPsychoHandler;

/**
 * 杰森模式专属疯魔心情 HUD。
 */
public final class JasonMoodHud {
    private static final Identifier BODY = NoellesRolesCore.id("hud/mood_psycho_jason");
    private static final Identifier HIT_BODY = NoellesRolesCore.id("hud/mood_psycho_jason_hit");
    private static final Identifier EYES = NoellesRolesCore.id("hud/mood_psycho_jason_eyes");
    private static final PsychoMoodHudStyle STYLE = new PsychoMoodHudStyle(
            (context, psycho) -> BODY,
            (context, psycho) -> HIT_BODY,
            (context, psycho) -> EYES,
            (context, psycho) -> Text.translatable("psycho_mode.noellesroles.jason.text"),
            (context, psycho) -> JasonConstants.ROLE_COLOR,
            (context, psycho) -> JasonConstants.ROLE_COLOR
    );

    private JasonMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerPsychoStyle(
                NoellesRolesCore.id("mood/jason_psycho"),
                MoodHudApi.DEFAULT_PRIORITY + 100,
                (context, psycho) -> JasonPsychoHandler.PROFILE_ID.equals(psycho.getProfileId()) ? STYLE : null
        );
    }
}
