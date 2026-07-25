package org.agmas.noellesroles.modifiers.allergic;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 过敏患者词条的集中数值。
 *
 * <p>StarryExpress 旧实现把这些值放在服务端 config 中；迁移后按 NoellesRoles 规则收口到常量，
 * 让玩法概率、持续时间和 guidebook 展示读取同一份数据。</p>
 */
public final class AllergicConstants {
    public static final int COLOR = 0x70FFA2;

    public static final int NOTHING_CHANCE = 3;
    public static final int INSTINCT_CHANCE = 1;
    public static final int SHIELD_CHANCE = 1;
    public static final int POISON_CHANCE = 1;

    public static final int INSTINCT_DURATION_TICKS = GameConstants.getInTicks(0, 3);
    public static final int POISON_ACCELERATION_MIN_TICKS = GameConstants.getInTicks(0, 5);
    public static final int POISON_ACCELERATION_MAX_TICKS = GameConstants.getInTicks(0, 15);

    public static final String ALLERGY_TYPE_FOOD = "food";
    public static final String ALLERGY_TYPE_DRINK = "drink";
    public static final String ALLERGY_TYPE_NONE = "none";

    private AllergicConstants() {
    }

    public static int totalChance() {
        return NOTHING_CHANCE + INSTINCT_CHANCE + SHIELD_CHANCE + POISON_CHANCE;
    }

    public static int guidebookTotalChance() {
        return totalChance();
    }

    public static int guidebookNothingChance() {
        return NOTHING_CHANCE;
    }

    public static int guidebookInstinctChance() {
        return INSTINCT_CHANCE;
    }

    public static int guidebookShieldChance() {
        return SHIELD_CHANCE;
    }

    public static int guidebookPoisonChance() {
        return POISON_CHANCE;
    }

    public static int guidebookInstinctDurationSeconds() {
        return INSTINCT_DURATION_TICKS / 20;
    }
}
