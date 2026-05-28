package org.agmas.noellesroles.roles.coward;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 胆小鬼相关的全部可调常量。
 */
public final class CowardConstants {
    private CowardConstants() {
    }

    /** 杀意感知在开局后延迟多久才正式生效，避免胆小鬼刚出生就开始被影响。 */
    public static final int SENSE_START_DELAY_TICKS = GameConstants.getInTicks(0, 30);
    /** 当“有效危险”消失后，需要持续保持安全状态多久，才会触发“危险暂时离你而去”的提示与回放。 */
    public static final int SENSE_LEAVE_GRACE_TICKS = GameConstants.getInTicks(0, 4);
    /** 镇静试剂的持续时间。 */
    public static final int SEDATIVE_DURATION_TICKS = GameConstants.getInTicks(0, 45);

    /** 杀意感知的最大检测半径，超过这个距离的玩家不会参与计算。 */
    public static final float SENSE_RADIUS = 15.0f;
    /** 第 1 梯度半径，最危险的近距离区间，距离越近反馈越强。 */
    public static final float BAND_1_RADIUS = 3.0f;
    /** 第 2 梯度半径。 */
    public static final float BAND_2_RADIUS = 7.0f;
    /** 第 3 梯度半径。 */
    public static final float BAND_3_RADIUS = 11.0f;
    /** 第 4 梯度半径，同时也是最外层有效距离。 */
    public static final float BAND_4_RADIUS = 15.0f;

    /** 非平民或平民位于第 1 梯度时，对“脉冲/心跳强度”结算的贡献值。 */
    public static final float BAND_1_PULSE = 1.00f;
    /** 非平民或平民位于第 2 梯度时，对“脉冲/心跳强度”结算的贡献值。 */
    public static final float BAND_2_PULSE = 0.75f;
    /** 非平民或平民位于第 3 梯度时，对“脉冲/心跳强度”结算的贡献值。 */
    public static final float BAND_3_PULSE = 0.50f;
    /** 非平民或平民位于第 4 梯度时，对“脉冲/心跳强度”结算的贡献值。 */
    public static final float BAND_4_PULSE = 0.25f;

    /** 非平民或平民位于第 1 梯度时，对 san 掉落倍率结算的贡献值。 */
    public static final float BAND_1_SAN = 0.30f;
    /** 非平民或平民位于第 2 梯度时，对 san 掉落倍率结算的贡献值。 */
    public static final float BAND_2_SAN = 0.25f;
    /** 非平民或平民位于第 3 梯度时，对 san 掉落倍率结算的贡献值。 */
    public static final float BAND_3_SAN = 0.20f;
    /** 非平民或平民位于第 4 梯度时，对 san 掉落倍率结算的贡献值。 */
    public static final float BAND_4_SAN = 0.10f;

    /** san 掉落倍率的最小下限，再多平民抵消也不会低于这个倍率。 */
    public static final float MIN_SAN_MULTIPLIER = 0.5f;
    /** san 掉落倍率的最大上限，再多危险叠加也不会高于这个倍率。 */
    public static final float MAX_SAN_MULTIPLIER = 2.0f;

    /** 心跳/FOV 脉冲触发间隔的最小值，数值越小表示最高频率越快。 */
    public static final int MIN_PULSE_INTERVAL_TICKS = 16;
    /** 心跳/FOV 脉冲触发间隔的最大值，数值越大表示最低频率越慢。 */
    public static final int MAX_PULSE_INTERVAL_TICKS = 80;
    /** 镇静试剂托盘粒子的刷新间隔，越大则粒子出现得越稀疏。 */
    public static final int SEDATIVE_TRAY_PARTICLE_INTERVAL_TICKS = 3;

    /** 胆小鬼商店内镇静试剂的金币售价。 */
    public static final int SEDATIVE_PRICE = 100;

    /** 胆小鬼手持左轮时的基础抖动强度，即使附近没人也会有这一层基础抖动。 */
    public static final float REVOLVER_BASE_SHAKE = 0.20f;
    /** 左轮镜头抖动的最低下限，哪怕被平民或安抚效果削弱，也不会完全不抖。 */
    public static final float REVOLVER_MIN_SHAKE = 0.08f;
    /** 左轮镜头抖动的最高上限，避免附近危险过多时抖动失控。 */
    public static final float REVOLVER_MAX_SHAKE = 0.62f;
    /** 危险/平民净脉冲值换算成左轮镜头抖动强度时所乘的系数。 */
    public static final float REVOLVER_SHAKE_SCALAR = 0.26f;

    /** 左轮抖动时，水平视角（yaw）最大偏移角度。 */
    public static final float REVOLVER_YAW_SHAKE_DEGREES = 3.2f;
    /** 左轮抖动时，垂直视角（pitch）最大偏移角度。 */
    public static final float REVOLVER_PITCH_SHAKE_DEGREES = 2.4f;
    /** 左轮抖动时，摄像机横向位置偏移幅度。 */
    public static final float REVOLVER_POSITION_SHAKE = 0.035f;

    /** FOV 脉冲最小时的缩放幅度。 */
    public static final float FOV_PULSE_MIN = 0.0125f;
    /** FOV 脉冲最大时的缩放幅度。 */
    public static final float FOV_PULSE_MAX = 0.05f;
    /** FOV 脉冲动画推进速度，越大则每次脉冲变化越急。 */
    public static final float FOV_PULSE_SPEED = 0.18f;
    /** FOV 主脉冲阶段的结束位置，用来控制一次脉冲前半段的曲线长度。 */
    public static final float FOV_PULSE_PRIMARY_END = 0.32f;
    /** FOV 次脉冲阶段的开始位置，用来控制一次脉冲后半段的回弹位置。 */
    public static final float FOV_PULSE_SECONDARY_START = 0.42f;
    /** FOV 次脉冲阶段的结束位置。 */
    public static final float FOV_PULSE_SECONDARY_END = 0.62f;
    /** 心跳声最小时的音量。 */
    public static final float HEARTBEAT_MIN_VOLUME = 0.45f;
    /** 心跳声最大时的音量。 */
    public static final float HEARTBEAT_MAX_VOLUME = 1.0f;
    /** 心跳声播放时的音高。 */
    public static final float HEARTBEAT_PITCH = 1.0f;

    /** 胆小鬼自带的左轮冷却倍率，0.5 代表冷却缩短为原本的一半。 */
    public static final float REVOLVER_COOLDOWN_FACTOR = 0.5f;
    /** 镇静效果额外提供的左轮冷却倍率，可与胆小鬼自身倍率叠加。 */
    public static final float SEDATIVE_REVOLVER_COOLDOWN_FACTOR = 0.5f;
    /** 左轮目标判定射线的最大距离，用于和镜头偏移后的瞄准判定保持一致。 */
    public static final float REVOLVER_RAYCAST_DISTANCE = 20.0f;
    /** 左轮抖动随时间变化时使用的时间缩放，越大则抖动节奏越快。 */
    public static final float REVOLVER_SHAKE_TIME_SCALE = 0.16f;
    /** 左轮抖动时，摄像机上下位置偏移幅度。 */
    public static final float REVOLVER_POSITION_VERTICAL_SHAKE = 0.024f;
    /** 左轮抖动时，摄像机前后位置偏移幅度。 */
    public static final float REVOLVER_POSITION_DEPTH_SHAKE = 0.014f;
}
