package org.agmas.noellesroles.client.roles.spring_trap;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.PsychoMoodHudStyle;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapPsychoHandler;

/**
 * 弹簧陷阱状态专属心情 HUD。
 */
public final class SpringTrapMoodHud {
    private static final Identifier BODY = NoellesRolesCore.id("hud/mood_spring_trap");
    private static final Identifier HIT_BODY = NoellesRolesCore.id("hud/mood_spring_trap_hit");
    private static final Identifier EYES = NoellesRolesCore.id("hud/mood_spring_trap_eyes");
    private static final PsychoMoodHudStyle STYLE = new PsychoMoodHudStyle(
            (context, psycho) -> BODY,
            (context, psycho) -> HIT_BODY,
            (context, psycho) -> EYES,
            (context, psycho) -> Text.translatable("psycho_mode.noellesroles.spring_trap.text"),
            (context, psycho) -> SpringTrapConstants.ROLE_COLOR,
            (context, psycho) -> SpringTrapConstants.ROLE_COLOR
    );

    private SpringTrapMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerPsychoStyle(
                NoellesRolesCore.id("mood/spring_trap_psycho"),
                MoodHudApi.DEFAULT_PRIORITY + 100,
                (context, psycho) -> SpringTrapPsychoHandler.PROFILE_ID.equals(psycho.getProfileId()) ? STYLE : null
        );
    }
}
