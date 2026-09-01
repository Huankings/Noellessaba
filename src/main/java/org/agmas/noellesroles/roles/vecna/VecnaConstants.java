package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.game.GameConstants;

/** 维克那所有可调玩法数值集中处。 */
public final class VecnaConstants {
    /** 维克那职业颜色：RGB(40,110,200)。 */
    public static final int ROLE_COLOR = 0x286EC8;
    /** 维克那每局最多生成数量。 */
    public static final int MAX_ROLE_COUNT = 1;
    /** 颠倒疯魔持续时间：45 秒。 */
    public static final int PSYCHO_DURATION_TICKS = GameConstants.getInTicks(0, 45);
    /** 购买颠倒疯魔后的冷却：4 分 10 秒。 */
    public static final int PSYCHO_COOLDOWN_TICKS = GameConstants.getInTicks(4, 10);
    /** 颠倒疯魔比默认疯魔贵 30 金币。 */
    public static final int PSYCHO_PRICE_BONUS = 30;
    /** 颠倒标记能力开局冷却：30 秒。 */
    public static final int ABILITY_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    /** 颠倒标记能力使用后冷却：60 秒。 */
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    /** 颠倒标记持续时间：35 秒。 */
    public static final int MARK_DURATION_TICKS = GameConstants.getInTicks(0, 35);
    /** 能力准心判定距离：2 格。 */
    public static final double MARK_RANGE_BLOCKS = 2.0D;
    /** 杀手侧标记反噬奖励：60 金币。 */
    public static final int KILLER_MARK_REWARD_COINS = 60;
    /** 好人侧标记反噬奖励：50 金币。 */
    public static final int CIVILIAN_MARK_REWARD_COINS = 50;
    /** 标记好人误伤好人的左轮保护奖励：100 金币。 */
    public static final int REVOLVER_MARK_REWARD_COINS = 100;
    /** 颠倒疯魔中每次致死伤害扣除的疯魔时间：15 秒。 */
    public static final int PSYCHO_FATAL_HIT_PENALTY_TICKS = GameConstants.getInTicks(0, 5);
    /** 游戏时间增加商品价格：70 金币。 */
    public static final int ADD_TIME_PRICE = 70;
    /** 游戏时间增加商品效果：30 秒。 */
    public static final int ADD_TIME_TICKS = GameConstants.getInTicks(0, 30);
    /** 维克那能力客户端 HUD 与回放使用的每秒 tick 数。 */
    public static final int TICKS_PER_SECOND = 20;
    /** 是否启用颠倒疯魔期间的反转色视角；默认开启，避免与其他视觉模组冲突。 */
    public static final boolean ENABLE_REVERSED_VIEW = true;

    private VecnaConstants() {
    }
}
