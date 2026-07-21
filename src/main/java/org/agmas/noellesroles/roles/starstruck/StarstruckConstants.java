package org.agmas.noellesroles.roles.starstruck;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 星界使者全部玩法数值。
 *
 * <p>这些数值来自 StarryExpress 1.3.2 的默认服务端配置。
 * 搬到 NoellesRoles 后不再读取 StarryExpress config，StarryExpress 绿皮书会反向读取这里的常量。</p>
 */
public final class StarstruckConstants {
    private StarstruckConstants() {
    }

    /** 职业颜色：沿用 StarryExpress 星界使者紫蓝色。 */
    public static final int ROLE_COLOR = 0x5747ff;
    /** StarryExpress 原实现是平民体力 + 100 tick，也就是多 5 秒冲刺时间。 */
    public static final int SPRINT_TIME_BONUS_TICKS = GameConstants.getInTicks(0, 5);

    /** 保留 StarryExpress 默认值：完成任务会减少当前星界能力冷却。 */
    public static final boolean TASK_REDUCES_COOLDOWN = true;
    public static final int TASK_COOLDOWN_REDUCTION_SECONDS = 10;
    public static final int TASK_COOLDOWN_REDUCTION_TICKS = GameConstants.getInTicks(0, TASK_COOLDOWN_REDUCTION_SECONDS);

    /** 用户确认过：星界使者保留原行为，开局没有能力冷却。 */
    public static final int START_COOLDOWN_TICKS = 0;

    public static final int ABILITY_COOLDOWN_SECONDS = 90;
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    public static final int ABILITY_DURATION_SECONDS = 25;
    public static final int ABILITY_DURATION_TICKS = GameConstants.getInTicks(0, ABILITY_DURATION_SECONDS);

    public static final boolean ABILITY_AFFECTS_MOVEMENT_SPEED = true;
    public static final float ABILITY_WALK_SPEED = 0.12F;
    public static final float ABILITY_SPRINT_SPEED = 0.15F;

    public static final int ABILITY_CAST_PARTICLE_COUNT = 75;
    public static final int ACTIVE_PARTICLE_MIN_COUNT = 1;
    public static final int ACTIVE_PARTICLE_MAX_COUNT = 2;

    /**
     * 给 StarryExpress 绿皮书读取用的运行时入口。
     * 直接跨模组引用 public static final int 会被 Java 编译器内联，后续只替换 NoellesRoles jar 时，
     * 绿皮书仍可能显示旧值；方法调用则会在运行时读取当前 NoellesRoles 里的常量。
     */
    public static int guidebookAbilityCooldownSeconds() {
        return ABILITY_COOLDOWN_SECONDS;
    }

    /** 同上，保持绿皮书显示与 NoellesRoles 当前星界能力持续时间一致。 */
    public static int guidebookAbilityDurationSeconds() {
        return ABILITY_DURATION_SECONDS;
    }

    /** 同上，保持绿皮书是否追加任务减冷却说明与 NoellesRoles 当前逻辑一致。 */
    public static boolean guidebookTaskReducesCooldown() {
        return TASK_REDUCES_COOLDOWN;
    }

    /** 同上，保持绿皮书任务减冷却数值与 NoellesRoles 当前逻辑一致。 */
    public static int guidebookTaskCooldownReductionSeconds() {
        return TASK_COOLDOWN_REDUCTION_SECONDS;
    }
}
