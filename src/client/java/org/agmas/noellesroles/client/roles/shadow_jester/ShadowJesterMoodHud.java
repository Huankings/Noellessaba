package org.agmas.noellesroles.client.roles.shadow_jester;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;

/**
 * 影子小丑心情 HUD 样式。
 */
public final class ShadowJesterMoodHud {
    private static final Identifier SHADOW_JESTER_MOOD = Identifier.of(NoellesRolesCore.MOD_ID, "hud/mood_shadow_jester");

    private ShadowJesterMoodHud() {
    }

    public static void register() {
        /*
         * 影子小丑是假心情职业，但图标和进度条颜色都需要保持职业辨识度。
         * 这里只替换贴图和心情条颜色，淡入淡出、任务文字收尾和警告动画继续交给 Wathe 心情 HUD 统一处理。
         */
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.SHADOW_JESTER, MoodHudStyle
                .builder(SHADOW_JESTER_MOOD)
                .barColor(ShadowJesterConstants.ROLE_COLOR)
                .build());
    }
}
