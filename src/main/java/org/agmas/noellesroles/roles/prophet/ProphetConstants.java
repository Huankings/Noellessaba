package org.agmas.noellesroles.roles.prophet;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 先知相关常量。
 *
 * <p>把数值集中在这里，后续你想平衡价格、冷却、距离时，
 * 只需要改这一处即可。</p>
 */
public final class ProphetConstants {

    /**
     * 水晶球商店售价。
     */
    public static final int CRYSTAL_BALL_PRICE = 25;

    /**
     * 先知发动揭露能力时需要消耗的金币。
     */
    public static final int REVEAL_COST = 125;

    /**
     * 先知揭露能力冷却时间：120 秒。
     */
    public static final int REVEAL_COOLDOWN_TICKS = GameConstants.getInTicks(2, 0);

    /**
     * 水晶球蓄力完成所需时长：0.1 秒。
     */
    public static final int CRYSTAL_BALL_CHARGE_TICKS = 2;

    /**
     * 水晶球对准玩家的检测距离。
     * 按照 wathe 原版 Knife 的 3 格距离实现。
     */
    public static final float CRYSTAL_BALL_RANGE = 3.0F;

    private ProphetConstants() {
    }
}
