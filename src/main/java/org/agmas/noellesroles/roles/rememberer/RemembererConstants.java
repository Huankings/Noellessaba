package org.agmas.noellesroles.roles.rememberer;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 追忆者整套功能的可调常量。
 *
 * <p>按用户要求，除职业 RGB 外，所有玩法数值都集中在这里，
 * 方便后续统一微调，不需要再去多个类里逐个搜索 magic number。</p>
 */
public final class RemembererConstants {

    private RemembererConstants() {
    }

    /** 追忆技能开局冷却。 */
    public static final int RECALL_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    /** 追忆技能正常冷却。 */
    public static final int RECALL_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    /** 追忆右键允许的基础距离。 */
    public static final double RECALL_DISTANCE = 2.0D;
    /** 服务端额外放宽一点距离，抵消网络抖动与碰撞箱误差。 */
    public static final double RECALL_SERVER_DISTANCE_TOLERANCE = 0.25D;
    /** 回忆书回溯窗口：最近 3 分钟。 */
    public static final int MEMORY_LOOKBACK_TICKS = GameConstants.getInTicks(3, 0);

    /** 狙击枪开局冷却。 */
    public static final int SNIPER_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    /** 狙击枪部署冷却。 */
    public static final int SNIPER_DEPLOY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 2);
    /** 狙击枪开火后冷却。 */
    public static final int SNIPER_SHOT_COOLDOWN_TICKS = GameConstants.getInTicks(0, 4);
    /** 狙击枪最大装填量。 */
    public static final int SNIPER_MAX_AMMO = 5;
    /** 狙击枪有效射程（格）。 */
    public static final double SNIPER_RANGE_BLOCKS = 60.0D;
    /** 子弹飞完整段射程所需 tick。 */
    public static final int SNIPER_TRAVEL_TICKS = 10;
    /** 每 tick 前进的距离。 */
    public static final double SNIPER_BLOCKS_PER_TICK = SNIPER_RANGE_BLOCKS / SNIPER_TRAVEL_TICKS;
    /** 射线从枪口前方略微偏移，避免一开始就和自己碰撞箱重叠。 */
    public static final double SNIPER_TRACE_START_OFFSET = 0.35D;
    /** 射线判定时给目标碰撞箱额外扩一点边。 */
    public static final double SNIPER_HITBOX_EXPANSION = 0.18D;
    /** 粒子沿线采样步长。 */
    public static final double SNIPER_PARTICLE_STEP = 0.35D;
    /** 开火后镜头上扬量，沿用左轮的观感但允许后续单独调。 */
    public static final float SNIPER_RECOIL_PITCH = 4.0F;
    /** 狙击枪持有时的移速倍率。 */
    public static final float SNIPER_SPEED_MULTIPLIER = 0.5F;
    /**
     * 手持狙击枪时，最终可输出的视角输入缩放比例。
     *
     * <p>这里直接决定“稳态下最多还能转多快”。
     * 之前数值虽然低，但因为旧算法会把损失的输入又通过残量慢慢补回去，
     * 持续转头时最终仍会接近原速，因此玩家会误以为完全没生效。
     * 现在改成真正有损的低通模型后，这个倍率就会稳定地体现为更笨重的瞄准速度。</p>
     */
    public static final double SNIPER_AIM_INPUT_SCALE = 0.40D;
    /**
     * 低通平滑保留系数。
     *
     * <p>值越大，上一帧输出保留越多，镜头越“发沉”；
     * 值越小，镜头越干脆。这里保留 85% 的上一帧输出，
     * 让镜头既明显迟缓，又不会拖到难以控制。</p>
     */
    public static final double SNIPER_AIM_INERTIA_DAMPING = 0.85D;
    /** 极小平滑残量直接归零，避免停止移动鼠标后镜头长期轻微漂移。 */
    public static final double SNIPER_AIM_EPSILON = 0.01D;

    /** 回忆书内文本统一使用的紫色。 */
    public static final int BOOK_TEXT_COLOR = 0xA86DFF;
    /** 追忆者开心状态下心情条颜色。 */
    public static final int MOOD_BAR_HAPPY_COLOR = 0xA86DFF;
    /** 追忆者中间状态下心情条颜色。 */
    public static final int MOOD_BAR_MID_COLOR = 0x4E217A;
    /** 追忆者低落状态下心情条颜色。 */
    public static final int MOOD_BAR_DEPRESSIVE_COLOR = 0x111111;

    /** 回忆书每行允许的近似宽度单位。 */
    public static final int BOOK_LINE_WIDTH_UNITS = 28;
    /** 回忆书标题居中的近似宽度单位。 */
    public static final int BOOK_TITLE_WIDTH_UNITS = 28;
    /** 回忆书每页允许的最大行数。 */
    public static final int BOOK_LINES_PER_PAGE = 13;

    /** 商店里一发狙击枪子弹的价格。 */
    public static final int SNIPER_BULLET_PRICE = 100;
}
