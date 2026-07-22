package org.agmas.noellesroles.roles.kidnapper;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 绑匪职业常量。
 */
public final class KidnapperConstants {
    public static final int ROLE_COLOR = 0xCC0066;
    public static final int KNOCKOUT_DRUG_PRICE = 75;
    public static final int ADDITIONAL_KILL_REWARD_COINS = 100;
    public static final int START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    public static final int KNOCKOUT_DRUG_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);
    public static final int CONTROL_DURATION_TICKS = GameConstants.getInTicks(0, 30);
    public static final float CONTROL_BREAK_DISTANCE = 5.0F;

    private KidnapperConstants() {
    }
}
