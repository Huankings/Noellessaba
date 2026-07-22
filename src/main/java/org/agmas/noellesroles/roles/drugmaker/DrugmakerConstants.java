package org.agmas.noellesroles.roles.drugmaker;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 制毒师职业常量。
 */
public final class DrugmakerConstants {
    public static final int ROLE_COLOR = 0x4C0099;
    public static final int MIN_KILLER_COUNT = 2;
    public static final int POISON_REWARD_COINS = 100;
    public static final int POISON_INJECTOR_PRICE = 100;
    public static final int BLOWGUN_PRICE = 175;
    public static final int START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    public static final int POISON_INJECTOR_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);
    public static final int BLOWGUN_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);
    public static final float POISON_INJECTOR_TARGET_RANGE = 3.0F;
    public static final float BLOWGUN_TARGET_RANGE = 15.0F;
    public static final int BLOWGUN_POISON_REDUCTION_MIN_TICKS = 100;
    public static final int BLOWGUN_POISON_REDUCTION_RANDOM_BOUND = 200;

    private DrugmakerConstants() {
    }
}
