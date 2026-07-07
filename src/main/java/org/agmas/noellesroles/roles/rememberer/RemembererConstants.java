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
    /** 狙击枪左键开镜过渡时长，单位为秒。当前 0.5 秒约等于 10 tick。 */
    public static final float SNIPER_SCOPE_OPEN_ANIMATION_SECONDS = 0.0F;//暂时设置为0代表马上开镜，原0.35
    /** 狙击枪松开左键收镜过渡时长，单位为秒。当前 0.5 秒约等于 10 tick。 */
    public static final float SNIPER_SCOPE_CLOSE_ANIMATION_SECONDS = 0.0F;//暂时设置为0代表马上关镜.原0.5
    /**
     * 收镜接近完成时的提前归零阈值。
     *
     * <p>客户端渲染会用上一 tick 和当前 tick 插值，如果严格等到 0 才结束，
     * 最后一小段黑镜/放大状态会多残留一拍，看起来像收镜后迟钝。
     * 这里在进度低于 3% 时直接切回普通状态，让手感更干脆。</p>
     */
    public static final float SNIPER_SCOPE_CLOSE_FINISH_PROGRESS = 0.03F;
    /**
     * 狙击镜最终可见范围的横向半径比例。
     *
     * <p>当前按你的需求设置成屏幕宽度的一半，也就是最终直径正好等于屏幕宽度。</p>
     */
    public static final float SNIPER_SCOPE_FINAL_HORIZONTAL_RADIUS_RATIO = 0.4F;
    /**
     * 开镜动画起始半径相对最终半径的比例。
     *
     * <p>设为 0.5 后，按下左键的第一段画面会从最终视野圈的一半开始放大。</p>
     */
    public static final float SNIPER_SCOPE_INITIAL_RADIUS_SCALE = 1.0F;//原0.5，现改为1也就是前后一致
    /**
     * 狙击镜遮罩的基准宽高比。
     *
     * <p>16:9 下横向半径和纵向半径相同，因此屏幕上的可见区是圆形；
     * 其他宽高比会按这个基准拉伸成椭圆，避免不同分辨率下视野圈观感完全跑偏。</p>
     */
    public static final float SNIPER_SCOPE_BASE_ASPECT_RATIO = 16.0F / 9.0F;
    /** 开镜完全完成时的 FOV 倍率，0.1F 是原版望远镜使用的放大强度。 */
    public static final float SNIPER_SCOPE_FOV_MULTIPLIER = 0.1F;
    /** 狙击镜十字线厚度，单位是 GUI 缩放后的像素。 */
    public static final int SNIPER_SCOPE_CROSSHAIR_THICKNESS = 1;
    /** 狙击镜遮罩颜色：纯黑且完全不透明。 */
    public static final int SNIPER_SCOPE_MASK_COLOR = 0xFF000000;
    /** 狙击镜中心十字线颜色。 */
    public static final int SNIPER_SCOPE_CROSSHAIR_COLOR = 0xFF000000;

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
    public static final int SNIPER_BULLET_PRICE = 75;
}
