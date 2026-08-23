package org.agmas.noellesroles.roles.robot;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 机器人职业常量。
 */
public final class RobotConstants {
    public static final int ROLE_COLOR = 0xC0C0C0;
    public static final int START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    public static final int ABILITY_DURATION_TICKS = GameConstants.getInTicks(0, 20);
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(1, 20);

    private RobotConstants() {
    }
}
