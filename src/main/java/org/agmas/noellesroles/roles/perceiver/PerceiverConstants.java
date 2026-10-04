package org.agmas.noellesroles.roles.perceiver;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 感知者的全部固定玩法数值。
 *
 * <p>职业机制中的时间、距离、视线容差、网络状态寿命和注册优先级都集中在这里，
 * 后续调整平衡时不需要在组件、网络包和客户端表现代码中寻找魔法数字。</p>
 */
public final class PerceiverConstants {
    /** 感知者职业色：RGB(220, 130, 70)。 */
    public static final int ROLE_COLOR = 0xDC8246;

    /** Harpy 随机分配时一局最多生成一名感知者。 */
    public static final int MAX_ROLE_COUNT = 1;

    /** 感知者使用普通左轮开枪后的冷却秒数。 */
    public static final int REVOLVER_COOLDOWN_SECONDS = 16;

    /** 感知者使用普通左轮开枪后的冷却 tick。 */
    public static final int REVOLVER_COOLDOWN_TICKS = GameConstants.getInTicks(0, REVOLVER_COOLDOWN_SECONDS);

    /**
     * 左轮冷却修正优先级。
     *
     * <p>使用较低优先级是为了让该 handler 在普通乘算修正之后把感知者左轮最终冷却固定为 16 秒。</p>
     */
    public static final int REVOLVER_COOLDOWN_PRIORITY = -1_000;

    /** 观察者能够触发感知者被动透视的最大距离，单位为格。 */
    public static final double PERCEPTION_DISTANCE = 64.0D;

    /** 最大距离的平方，服务端逐 tick 判定时避免重复开平方。 */
    public static final double PERCEPTION_DISTANCE_SQUARED = PERCEPTION_DISTANCE * PERCEPTION_DISTANCE;

    /**
     * “感知者处于观察者视角范围内”的视野锥半角，单位为度。
     *
     * <p>沿用 Stalker 凝视机制的 80 度半角语义：玩家不需要把准心压在感知者身体上，
     * 只要感知者仍位于其前方较宽的视角范围内即可持续触发。</p>
     */
    public static final double GAZE_HALF_ANGLE_DEGREES = 60.0D;

    /**
     * 视野锥点积阈值，由半角常量统一推导。
     *
     * <p>后续只需调整 {@link #GAZE_HALF_ANGLE_DEGREES}，无需手动重新计算向量点积阈值。</p>
     */
    public static final double GAZE_DOT_THRESHOLD = Math.cos(Math.toRadians(GAZE_HALF_ANGLE_DEGREES));

    /** 观察者必须连续看向感知者的秒数。 */
    public static final double REQUIRED_GAZE_SECONDS = 1.5D;

    /** Minecraft 每秒固定推进的游戏 tick 数，仅用于把带小数的凝视秒数换算为 tick。 */
    public static final int TICKS_PER_SECOND = 20;

    /** 观察者必须连续看向感知者的 tick 数。 */
    public static final int REQUIRED_GAZE_TICKS = (int) Math.round(REQUIRED_GAZE_SECONDS * TICKS_PER_SECOND);

    /** 背包按钮成功选择后，目标可被动透视感知者的持续秒数。 */
    public static final int SELECTED_EFFECT_DURATION_SECONDS = 30;

    /** 背包按钮成功选择后，目标可被动透视感知者的持续 tick。 */
    public static final int SELECTED_EFFECT_DURATION_TICKS = GameConstants.getInTicks(0, SELECTED_EFFECT_DURATION_SECONDS);

    /** 成功选择存活玩家后的能力冷却秒数。 */
    public static final int SUCCESS_COOLDOWN_SECONDS = 90;

    /** 成功选择存活玩家后的能力冷却 tick。 */
    public static final int SUCCESS_COOLDOWN_TICKS = GameConstants.getInTicks(
            SUCCESS_COOLDOWN_SECONDS / 60,
            SUCCESS_COOLDOWN_SECONDS % 60
    );

    /** 选择非存活玩家失败后的能力冷却秒数。 */
    public static final int FAILURE_COOLDOWN_SECONDS = 60;

    /** 选择非存活玩家失败后的能力冷却 tick。 */
    public static final int FAILURE_COOLDOWN_TICKS = GameConstants.getInTicks(
            FAILURE_COOLDOWN_SECONDS / 60,
            FAILURE_COOLDOWN_SECONDS % 60
    );

    /** 每完成一个任务减少的能力冷却秒数。 */
    public static final int TASK_COOLDOWN_REDUCTION_SECONDS = 15;

    /** 每完成一个任务减少的能力冷却 tick。 */
    public static final int TASK_COOLDOWN_REDUCTION_TICKS = GameConstants.getInTicks(0, TASK_COOLDOWN_REDUCTION_SECONDS);

    /** 客户端主动本能/被动描边状态的心跳上报间隔 tick。 */
    public static final int AWARENESS_REPORT_INTERVAL_TICKS = 10;

    /**
     * 服务端接受一次客户端感知状态报告的最长 tick。
     *
     * <p>该值大于两个心跳周期，允许短暂抖动；客户端停止上报或掉线后，旧状态会自动失效。</p>
     */
    public static final int AWARENESS_REPORT_TIMEOUT_TICKS = 30;

    /**
     * 感知者描边 provider 的优先级。
     *
     * <p>高于普通职业色和能力标记，保证选中目标看见的始终是感知者职业色；
     * 同时远低于时间狭缝、尸体伪装等全局隐藏规则，不绕过不可见性保护。</p>
     */
    public static final int INSTINCT_HIGHLIGHT_PRIORITY = 500;

    /** 背包按钮成功音效音量。 */
    public static final float SUCCESS_SOUND_VOLUME = 1.0F;

    /** 背包按钮成功音效音调。 */
    public static final float SUCCESS_SOUND_PITCH = 1.0F;

    /** 背包按钮失败音效音量。 */
    public static final float FAILURE_SOUND_VOLUME = 1.0F;

    /** 背包按钮失败音效音调。 */
    public static final float FAILURE_SOUND_PITCH = 1.0F;

    private PerceiverConstants() {
    }
}
