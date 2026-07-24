package org.agmas.noellesroles.client.roles.robot;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.client.mood.MoodHudApi;
import dev.doctor4t.wathe.api.client.mood.MoodHudStyle;
import net.minecraft.util.Identifier;

/**
 * 机器人心情 HUD 样式。
 */
public final class RobotMoodHud {
    private static final Identifier ROBOT_MOOD = Identifier.of("wathe", "hud/mood_happy");

    private RobotMoodHud() {
    }

    public static void register() {
        /*
         * 机器人是 FAKE mood 类型，但视觉上复用平民开心图标与 HSV 心情条，
         * 这样不会因为假心情类型显示成杀手风格。
         */
        MoodHudApi.registerRoleStyle(NoellesRoleRegistry.ROBOT, MoodHudStyle
                .builder(ROBOT_MOOD)
                .hsvMoodBar()
                .build());
    }
}
