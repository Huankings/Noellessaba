package org.agmas.noellesroles.client.roles.jester;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.util.Identifier;

public final class JesterMoodHud {
    private static final Identifier JESTER_MOOD = Identifier.of(NoellesRolesCore.MOD_ID, "hud/mood_jester");

    private JesterMoodHud() {
    }

    public static void register() {
        /*
         * 狂信者是假心情职业，任务完成后应沿用 Wathe 原本的假心情条动画：
         * 任务文字先淡出，心情条宽度被 moodTextWidth 平滑拉回默认长度，然后再随 HUD alpha 淡出。
         *
         * 这里不再使用 barVisibleWhen(MoodHudContext::hasMoodTasks)。
         * hasMoodTasks 只表示服务端当前是否还有活跃任务；任务刚完成但 HUD 仍在淡出时它会立刻变 false，
         * 直接用它控制心情条会跳过 Wathe 的收尾动画，变成“任务一做完条就直接隐藏”。
         */
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.JESTER, MoodHudStyle
                .builder(JESTER_MOOD)
                .barColor(NoellesRoleRegistry.JESTER.color())
                .build());
    }
}
