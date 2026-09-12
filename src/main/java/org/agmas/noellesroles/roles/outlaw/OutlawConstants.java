package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.game.GameConstants;

/** 亡命徒职业的全部玩法数值。 */
public final class OutlawConstants {
    /** 亡命徒职业色。 */
    public static final int ROLE_COLOR = 0x9F0000;
    /** 亡命徒本能透视使用的灰色。 */
    public static final int INSTINCT_COLOR = 0x808080;
    /** 亡命时刻初始持续时间。 */
    public static final int OUTLAW_TIME_TICKS = GameConstants.getInTicks(1, 20);
    /** 每次有效击杀给亡命时刻增加的时间。 */
    public static final int KILL_TIME_BONUS_TICKS = GameConstants.getInTicks(0, 15);
    /** 每累计多少次有效击杀获得一层护盾。 */
    public static final int KILLS_PER_SHIELD = 7;
    /** 复活时每多少名存活玩家提供一层初始护盾。 */
    public static final int ALIVE_PLAYERS_PER_INITIAL_SHIELD = 6;
    /** 初始护盾的最大层数；负数表示不限制。 */
    public static final int MAX_INITIAL_SHIELD_LAYERS = 3;
    /** 击杀累计护盾功能默认开启。 */
    public static final boolean KILL_SHIELD_ENABLED = true;
    /** 击杀累计护盾的最大层数；负数表示不限制。 */
    public static final int MAX_KILL_SHIELD_LAYERS = 3;
    /** 亡命徒左轮固定冷却。 */
    public static final int REVOLVER_COOLDOWN_TICKS = GameConstants.getInTicks(0, 10);
    /** 复活停电开始恢复的最短时间。 */
    public static final int BLACKOUT_MIN_SECONDS = 3;
    /** 复活停电完全恢复的最长时间。 */
    public static final int BLACKOUT_MAX_SECONDS = 10;
    /** 亡命徒复活后向全场暴露位置的发光持续时间。 */
    public static final int REVIVAL_GLOW_TICKS = GameConstants.getInTicks(0, 15);
    /** 亡命徒复活时的初始缓慢等级。 */
    public static final int INITIAL_SLOWNESS_AMPLIFIER = 4;
    /** 每个缓慢等级持续 2 秒。 */
    public static final int SLOWNESS_LEVEL_DURATION_TICKS = GameConstants.getInTicks(0, 2);
    /** 亡命徒复活时是否给自己也施加全场失明规则。 */
    public static final boolean BLIND_OUTLAW_DURING_REVIVAL_BLACKOUT = true;
    /** 顶部亡命时间 HUD 的低时间警告关闭值。 */
    public static final int TIME_HUD_NO_LOW_WARNING = -1;
    /** HUD 将 tick 向上换算为秒时使用的每秒 tick 数。 */
    public static final int HUD_TICKS_PER_SECOND = 20;
    /** HUD 向上取整秒数时使用的偏移 tick 数。 */
    public static final int HUD_ROUNDING_OFFSET_TICKS = 19;
    /** 亡命徒实际移动速度相对 Wathe 基础速度的倍率。 */
    public static final float SPEED_MULTIPLIER = 2.8F;

    private OutlawConstants() {
    }

    /** 按职业配置限制护盾层数。 */
    public static int clampShieldLayers(int layers, int maximum) {
        if (maximum < 0) {
            return Math.max(0, layers);
        }
        return Math.max(0, Math.min(layers, maximum));
    }
}
