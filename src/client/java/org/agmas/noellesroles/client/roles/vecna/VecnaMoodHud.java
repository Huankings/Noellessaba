package org.agmas.noellesroles.client.roles.vecna;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.PsychoMoodHudStyle;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.vecna.VecnaConstants;
import org.agmas.noellesroles.roles.vecna.VecnaPsychoHandler;

/** 维克那颠倒疯魔专属心情图标、字样和职业色。 */
public final class VecnaMoodHud {
    private static final Identifier BODY = NoellesRolesCore.id("hud/mood_psycho_vecna");
    private static final Identifier HIT_BODY = NoellesRolesCore.id("hud/mood_psycho_vecna_hit");
    private static final Identifier EYES = NoellesRolesCore.id("hud/mood_psycho_vecna_eyes");
    private static final PsychoMoodHudStyle STYLE = new PsychoMoodHudStyle(
            (context, psycho) -> BODY, (context, psycho) -> HIT_BODY, (context, psycho) -> EYES,
            (context, psycho) -> Text.translatable("psycho_mode.noellesroles.vecna.text"),
            (context, psycho) -> VecnaConstants.ROLE_COLOR,
            (context, psycho) -> VecnaConstants.ROLE_COLOR);
    private VecnaMoodHud() {}
    public static void register() {
        MoodHudApi.registerPsychoStyle(NoellesRolesCore.id("mood/vecna_psycho"), MoodHudApi.DEFAULT_PRIORITY + 100,
                (context, psycho) -> VecnaPsychoHandler.PROFILE_ID.equals(psycho.getProfileId()) ? STYLE : null);
    }
}
