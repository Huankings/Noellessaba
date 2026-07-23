package org.agmas.noellesroles.roles.convener;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 召集者的固定玩法数值。
 */
public final class ConvenerConstants {
    public static final int ROLE_COLOR = 0x5734e5;

    public static final int SUMMON_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    public static final int SUMMON_MORPH_TICKS = GameConstants.getInTicks(0, 30);
    public static final int SUMMON_TIME_BONUS_TICKS = GameConstants.getInTicks(1, 0);
    public static final int SUMMON_SPEED_DURATION_TICKS = GameConstants.getInTicks(0, 15);

    /**
     * 保留 StupidExpress 的实际数值：最终速度 = 原速度 * (1 + 0.7)。
     *
     * <p>原注释写的是 2 倍，但代码值是 0.7D；这里以已上线行为为准，避免搬运时暗改平衡。</p>
     */
    public static final double SUMMON_SPEED_MULTIPLIER_BONUS = 0.7D;

    public static final boolean COUNTER_SHIELD_ENABLED = false;
    public static final int TASKS_PER_COUNTER_SHIELD = 4;

    public static final int WEAPON_ITEM_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);
    public static final int PSYCHO_MODE_COOLDOWN_TICKS = GameConstants.getInTicks(1, 10);
    public static final int BLACKOUT_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    public static final int SPECIAL_EVENT_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);

    private ConvenerConstants() {
    }
}
