package org.agmas.noellesroles.roles.angel;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 天使职业的全部可调常量。
 *
 * <p>你要求后续数值都要方便调整，因此这里把和：
 * 1. san 下降倍率；
 * 2. 安抚范围 / 持续时间 / 冷却；
 * 3. 守护判定距离 / 冷却；
 * 4. 粒子持续时间与密度；
 *
 * 相关的数值统一集中在这里。</p>
 */
public final class AngelConstants {
    private AngelConstants() {
    }

    /**
     * 天使自身的 san 下降倍率。
     * 0.5 表示只有普通好人的一半速度。
     */
    public static final float MOOD_DRAIN_MULTIPLIER = 0.5f;

    /**
     * 安抚技能的生效半径。
     */
    public static final float SOOTHE_RADIUS = 5.0f;

    public static final float SOOTHE_RADIUS_SQUARED = SOOTHE_RADIUS * SOOTHE_RADIUS;

    /**
     * 安抚效果持续时间：30 秒。
     */
    public static final int SOOTHE_PROTECTION_TICKS = GameConstants.getInTicks(0, 30);

    /**
     * 安抚技能冷却：90 秒。
     */
    public static final int SOOTHE_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);

    /**
     * 守护技能需要真正对准玩家的最大距离：2 格。
     */
    public static final float GUARD_RANGE = 2.0f;

    /**
     * 守护技能冷却：30 秒。
     */
    public static final int GUARD_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    /**
     * 安抚粒子持续时间：5 秒。
     */
    public static final int SOOTHE_PARTICLE_TICKS = GameConstants.getInTicks(0, 5);

    /**
     * 安抚粒子的单 tick 最大生成数量。
     * 会随着剩余时间线性递减，形成“慢慢消散”的观感。
     */
    public static final int SOOTHE_PARTICLE_MAX_COUNT = 14;

    /**
     * 安抚粒子的垂直扩散高度。
     */
    public static final double SOOTHE_PARTICLE_VERTICAL_SPREAD = 0.9;

    /**
     * 安抚粒子的缓慢漂浮速度。
     */
    public static final double SOOTHE_PARTICLE_SPEED = 0.015;
}
