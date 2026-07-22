package org.agmas.noellesroles.roles.hunter;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 追猎者职业常量。
 */
public final class HunterConstants {
    public static final int ROLE_COLOR = 0x663300;
    public static final int ABILITY_PRICE = 15;
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 0);
    public static final int HUNTING_KNIFE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);
    public static final int HUNTING_KNIFE_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    public static final int HUNTING_KNIFE_MAX_USE_TICKS = 200;
    public static final int HUNTING_KNIFE_MIN_USE_TICKS = 10;
    public static final int HUNTING_KNIFE_CLIENT_SEND_GRACE_TICKS = 5;
    public static final int HUNTING_KNIFE_SERVER_RELEASE_GRACE_TICKS = 20;
    public static final float HUNTING_KNIFE_TARGET_RANGE = 3.0F;
    public static final float HUNTER_SPRINT_MOVEMENT_SPEED = 0.17F;
    public static final double HUNTER_SPRINT_FOV_MULTIPLIER = 1.0D;
    public static final int NORMAL_KNIFE_PRICE_NUMERATOR = 7;
    public static final int NORMAL_KNIFE_PRICE_DENOMINATOR = 4;

    private HunterConstants() {
    }
}
