package org.agmas.noellesroles.client.roles.cook;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.PsychoMoodHudStyle;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.agmas.noellesroles.roles.cook.CookPsychoHandler;

/**
 * 厨师疯魔专属心情 HUD。
 *
 * <p>Wathe 会根据护盾是否还存在自动在 body / hitBody 之间切换；
 * 本类只负责把厨师自己的图标、跑马文字和颜色交给 MoodHudApi。</p>
 */
public final class CookMoodHud {
    private static final Identifier BODY = NoellesRolesCore.id("hud/mood_psycho_cook");
    private static final Identifier HIT_BODY = NoellesRolesCore.id("hud/mood_psycho_cook_hit");
    private static final Identifier EYES = NoellesRolesCore.id("hud/mood_psycho_cook_eyes");
    private static final PsychoMoodHudStyle STYLE = new PsychoMoodHudStyle(
            (context, psycho) -> BODY,
            (context, psycho) -> HIT_BODY,
            (context, psycho) -> EYES,
            (context, psycho) -> Text.translatable("psycho_mode.noellesroles.cook.text"),
            (context, psycho) -> CookConstants.PSYCHO_COOK_TEXT_COLOR,
            (context, psycho) -> CookConstants.PSYCHO_COOK_TIMER_BAR_COLOR
    );

    private CookMoodHud() {
    }

    public static void register() {
        MoodHudApi.registerPsychoStyle(
                NoellesRolesCore.id("mood/cook_psycho"),
                MoodHudApi.DEFAULT_PRIORITY + 100,
                (context, psycho) -> CookPsychoHandler.PROFILE_ID.equals(psycho.getProfileId()) ? STYLE : null
        );
    }
}
