package org.agmas.noellesroles.modifiers.dual_personality;

/**
 * 双重人格词条的固定数值。
 *
 * <p>除“随机刷新所需最少人数”按用户确认保留为 config 外，其余 StupidExpress config/default
 * 都迁移到这里，避免玩法平衡值散落在 manager 或 mixin 里。</p>
 */
public final class DualPersonalityConstants {
    public static final int SWITCH_INTERVAL_TICKS = 60 * 20;
    public static final int DOUBLE_ACTIVE_BASE_TICKS = 40 * 20;
    public static final int DOUBLE_ACTIVE_KILL_BONUS_TICKS = 10 * 20;
    public static final int DOUBLE_ACTIVE_KNIFE_COOLDOWN_TICKS = 20;
    public static final int DOUBLE_ACTIVE_SPEED_PERCENT = 50;
    /*
     * 旧客户端 movement mixin 实际写死的是 1.9 倍，而不是上面的 50%。
     * 迁到 Wathe MovementApi 后继续保留这个倍率，避免双活阶段移动手感突然变化。
     */
    public static final float DOUBLE_ACTIVE_SPEED_MULTIPLIER = 1.9F;
    public static final int COLOR = 0x7633db;
    public static final int INITIAL_ROLE_MESSAGE_DELAY_TICKS = 2 * 20;
    public static final int INITIAL_CAMERA_SYNC_TICKS = 5 * 20;
    public static final int DEFAULT_MIN_RANDOM_PLAYER_COUNT = 8;
    public static final boolean WIN_WITH_KILLERS = false;
    public static final boolean WIN_WITH_CIVILIANS = false;

    private DualPersonalityConstants() {
    }
}
