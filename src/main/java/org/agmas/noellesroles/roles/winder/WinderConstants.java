package org.agmas.noellesroles.roles.winder;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 风灵师和风之印记共用常量。
 * 后续如果你要调平衡，优先改这里会最省事。
 */
public final class WinderConstants {

    /**
     * 没对准玩家时，自我烙印需要蓄力 1 秒。
     */
    public static final int MARK_SELF_CHARGE_TICKS = GameConstants.getInTicks(0, 1);

    /**
     * 烙印存在 180 秒。
     */
    public static final int MARK_DURATION_TICKS = GameConstants.getInTicks(3, 0);

    /**
     * 附近举刀触发保命的判定半径。
     */
    public static final double MARK_KNIFE_TRIGGER_RADIUS = 5.0D;
    public static final double MARK_KNIFE_TRIGGER_RADIUS_SQUARED =
            MARK_KNIFE_TRIGGER_RADIUS * MARK_KNIFE_TRIGGER_RADIUS;

    /**
     * 漂浮 15 级，Minecraft 内部放大器从 0 开始计，所以这里写 14。
     */
    public static final int MARK_ESCAPE_LEVITATION_AMPLIFIER = 14;

    /**
     * 保命升空持续 1 秒。
     */
    public static final int MARK_ESCAPE_LEVITATION_TICKS = GameConstants.getInTicks(0, 1);

    /**
     * 风灵师主动技能的漂浮总时长：10 秒。
     */
    public static final int FLOAT_DURATION_TICKS = GameConstants.getInTicks(0, 10);

    /**
     * 主动技能给予的是 levitation 2 级，所以放大器写 1。
     */
    public static final int FLOAT_LEVITATION_AMPLIFIER = 1;

    /**
     * 为了支持“提前结束”而不误删其他来源的漂浮，
     * 我们采用短时刷新方式维持效果。
     */
    public static final int FLOAT_EFFECT_REFRESH_TICKS = 2;

    /**
     * 冷却 = 实际漂浮时长 * 6。
     */
    public static final int FLOAT_COOLDOWN_MULTIPLIER = 6;

    /**
     * 风灵师主动技能的最小冷却时间。
     *
     * <p>当提前结束漂浮时，如果按“实际漂浮时间 * 倍率”算出来的冷却过短，
     * 则至少也要遵守这里的最小冷却值。
     * 当该值小于等于 0 时，表示关闭这个限制，完全沿用动态冷却。</p>
     *
     * <p>当前按你的需求设置为 20 秒。</p>
     */
    public static final int FLOAT_MINIMUM_COOLDOWN_TICKS = GameConstants.getInTicks(0, 20);

    private WinderConstants() {
    }
}
