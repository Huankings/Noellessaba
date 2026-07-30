package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 变形怪相关常量。
 *
 * <p>这里统一收口变形持续时间与冷却时间，
 * 方便你后续做平衡时只改一处即可。</p>
 */
public final class MorphlingConstants {

    /**
     * 变形持续时间：1 分 10 秒。
     */
    public static final int MORPH_DURATION_TICKS = GameConstants.getInTicks(1, 10);

    /**
     * 变形结束后冷却：5 秒。
     */
    public static final int MORPH_COOLDOWN_TICKS = GameConstants.getInTicks(0, 5);

    /**
     * 变形试剂采样 / 标记的有效距离。
     *
     * <p>这个数值按 SparkStrength66 的增强方案保留。试剂是近身陷害道具，
     * 距离过远会让“采样 -> 标记”的风险变得太低。</p>
     */
    public static final double REAGENT_TARGET_RANGE = 1.5D;

    /**
     * 变形试剂触发后的持续时间：60 秒。
     */
    public static final int REAGENT_ACTIVE_DURATION_TICKS = GameConstants.getInTicks(1, 0);

    /**
     * 变形试剂在变形怪杀手商店中的售价。
     */
    public static final int MORPH_REAGENT_PRICE = 25;

    /**
     * 变形怪自己处于伪装状态时击杀玩家获得的额外金币。
     */
    public static final int SELF_MORPH_KILL_REWARD = 30;

    /**
     * 变形怪击杀“自己正在伪装成的目标”时获得的额外金币。
     */
    public static final int SELF_MORPH_TARGET_KILL_REWARD = 60;

    /**
     * 被试剂标记的其他玩家在伪装期间参与击杀或死亡时，标记者获得的金币。
     */
    public static final int OTHER_MARK_EVENT_REWARD = 50;

    /**
     * 商店条目稳定 id，仅用于 NoellesRoles 侧定位和日志，不沿用 SparkStrength 命名空间。
     */
    public static final String MORPH_REAGENT_ENTRY_ID = "morph_reagent";

    /**
     * 试剂标记的本能高亮优先级。
     *
     * <p>它要高于普通职业本能颜色，保证变形怪能稳定看到自己布下的标记；
     * 但低于时间狭缝、召集者压制等强制视觉规则。</p>
     */
    public static final int MARK_HIGHLIGHT_PRIORITY = 240;

    private MorphlingConstants() {
    }
}
