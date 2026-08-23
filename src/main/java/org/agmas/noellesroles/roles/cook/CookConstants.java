package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 厨师职业的所有玩法数值集中在这里。
 *
 * <p>kinssaba 原先有一部分数值写在 config，一部分直接写在物品和数据包里。
 * 搬进 NoellesRoles 后统一放到职业常量，后续调平衡时不需要再到处翻实现类。</p>
 */
public final class CookConstants {
    /**
     * 厨师职业色，也作为厨师疯魔倒计时条的绿色和投喂 actionbar 颜色。
     */
    public static final int ROLE_COLOR = 0xCCFF99;

    /**
     * 厨师透视“刚吃过东西的玩家”的持续时间。
     */
    public static final int EAT_MARK_TICKS = GameConstants.getInTicks(0, 40);

    /**
     * 平底锅普通命中后的物品冷却。
     */
    public static final int PAN_COOLDOWN_TICKS = GameConstants.getInTicks(0, 25);

    /**
     * 平底锅和飞锅共同使用的眩晕时长。
     */
    public static final int PAN_STUN_TICKS = GameConstants.getInTicks(0, 5);

    /**
     * 平底锅右键至少蓄力多少 tick 后才允许发送命中包。
     */
    public static final int PAN_MIN_USE_TICKS = 10;

    /**
     * 平底锅客户端松手包发送的宽限 tick，避免刚松手一瞬间被最大使用时间边界吞掉。
     */
    public static final int PAN_CLIENT_SEND_GRACE_TICKS = 5;

    /**
     * 平底锅右键蓄力动作的最大 tick，超过后原版会自动停止使用。
     */
    public static final int PAN_MAX_USE_TICKS = 100;

    /**
     * 服务端判定平底锅命中时允许的最大蓄力 tick，用于过滤过期包。
     */
    public static final int PAN_MAX_USE_TICKS_FOR_HIT = PAN_MAX_USE_TICKS - PAN_CLIENT_SEND_GRACE_TICKS - 1;

    /**
     * 平底锅和厨师投喂对准玩家的交互距离。
     */
    public static final float PAN_TARGET_RANGE = 3.0F;

    /**
     * 平底锅在厨师商店中的价格。
     */
    public static final int PAN_SHOP_PRICE = 85;

    /**
     * 厨师随机食物商店图标的价格，沿用旧熟食价格。
     */
    public static final int COOKED_FOOD_SHOP_PRICE = 5;

    /**
     * 厨师自己完成心情任务时的普通任务收入基准。
     */
    public static final int TASK_INCOME = 50;

    /**
     * 厨师把食物投喂给他人并帮对方完成“吃东西”任务时获得的协助奖励。
     */
    public static final int FEED_HELP_BONUS = 75;

    /**
     * 厨师投喂右键射线检测的距离，和厨师近身平底锅交互保持一致。
     */
    public static final float FEED_TARGET_RANGE = PAN_TARGET_RANGE;

    /**
     * 飞锅在厨师商店中的价格。
     */
    public static final int THROWING_PAN_SHOP_PRICE = 145;

    /**
     * 厨师疯魔商店图标的价格。
     */
    public static final int PSYCHO_COOK_SHOP_PRICE = 235;

    /**
     * 飞锅和疯魔飞锅使用弓形蓄力动作时允许持续按住的最大 tick。
     */
    public static final int THROWING_PAN_MAX_USE_TICKS = 72000;

    /**
     * 飞锅蓄力曲线中，多少 tick 视作 1 秒蓄力单位。
     */
    public static final float THROWING_PAN_CHARGE_UNIT_TICKS = 20.0F;

    /**
     * 飞锅蓄力曲线的一次项倍率，复刻飞斧的手感。
     */
    public static final float THROWING_PAN_POWER_LINEAR_MULTIPLIER = 2.0F;

    /**
     * 飞锅蓄力曲线的归一化除数，复刻飞斧的手感。
     */
    public static final float THROWING_PAN_POWER_DIVISOR = 3.0F;

    /**
     * 飞锅蓄力曲线的最大力度。
     */
    public static final float THROWING_PAN_MAX_POWER = 1.0F;

    /**
     * 飞锅允许投掷的最小力度，低于该值视为误触。
     */
    public static final float THROWING_PAN_MIN_POWER = 0.25F;

    /**
     * 飞锅最低发射速度，低蓄力但达到阈值时也能飞出。
     */
    public static final float THROWING_PAN_BASE_VELOCITY = 0.4F;

    /**
     * 飞锅随蓄力增加的额外速度倍率，复刻飞斧速度曲线。
     */
    public static final float THROWING_PAN_POWER_VELOCITY_MULTIPLIER = 2.0F;

    /**
     * 飞锅投掷散布，值越小越贴近准星中心。
     */
    public static final float THROWING_PAN_VELOCITY_DIVERGENCE = 1.0F;

    /**
     * 飞锅生成时放在玩家眼睛高度下方的偏移，避免刚生成就卡进视角或头部。
     */
    public static final double THROWING_PAN_SPAWN_EYE_Y_OFFSET = 0.1D;

    /**
     * 飞锅每 tick 扫描贯穿路径时，对整段飞行包围盒追加的搜索扩张量。
     */
    public static final double THROWING_PAN_HIT_SCAN_BOX_EXPAND = 1.6D;

    /**
     * 飞锅对玩家命中盒额外增加的半径，提升高速飞行时的擦边命中稳定性。
     */
    public static final double THROWING_PAN_PLAYER_HITBOX_EXPAND = 0.3D;

    /**
     * 飞锅命中玩家后保留的速度倍率，低于 1 表示轻微减速后继续贯穿。
     */
    public static final double THROWING_PAN_HIT_VELOCITY_MULTIPLIER = 0.9D;

    /**
     * 普通飞锅实体最长存在时间，和飞斧一致用于兜底清理。
     */
    public static final int THROWING_PAN_MAX_LIFETIME_TICKS = GameConstants.getInTicks(2, 0);

    /**
     * 疯魔飞锅落地插入方块后保留的时间，时间到自动清理。
     */
    public static final int PSYCHO_THROWING_PAN_STUCK_LIFETIME_TICKS = GameConstants.getInTicks(0, 10);

    /**
     * 飞锅投掷音量。
     */
    public static final float THROWING_PAN_THROW_SOUND_VOLUME = 1.0F;

    /**
     * 飞锅投掷音高。
     */
    public static final float THROWING_PAN_THROW_SOUND_PITCH = 1.0F;

    /**
     * 飞锅命中方块音量。
     */
    public static final float THROWING_PAN_GROUND_HIT_SOUND_VOLUME = 1.0F;

    /**
     * 飞锅命中方块音高。
     */
    public static final float THROWING_PAN_GROUND_HIT_SOUND_PITCH = 1.0F;

    /**
     * 飞锅命中玩家时复用平底锅击晕音效的音量。
     */
    public static final float THROWING_PAN_PLAYER_HIT_SOUND_VOLUME = 0.8F;

    /**
     * 飞锅命中玩家时复用平底锅击晕音效的音高。
     */
    public static final float THROWING_PAN_PLAYER_HIT_SOUND_PITCH = 0.8F;

    /**
     * 飞锅实体渲染缩放，和飞斧保持同等视觉体量。
     */
    public static final float THROWING_PAN_RENDER_SCALE = 1.6F;

    /**
     * 飞锅飞行时每 tick 绕 Y 轴旋转的角度。
     */
    public static final float THROWING_PAN_RENDER_Y_ROTATION_PER_TICK = 8.0F;

    /**
     * 飞锅飞行时 Z 轴旋转相对 Y 轴旋转的倍率。
     */
    public static final float THROWING_PAN_RENDER_Z_ROTATION_MULTIPLIER = 0.7F;

    /**
     * 飞锅插在方块上时用于贴近方块面的位移。
     */
    public static final float THROWING_PAN_STUCK_RENDER_OFFSET = 0.35F;

    /**
     * 飞锅插在方块侧面时向内倾斜的角度。
     */
    public static final float THROWING_PAN_STUCK_RENDER_SIDE_TILT_DEGREES = -50.0F;

    /**
     * 厨师疯魔 profile 的持续时间。
     */
    public static final int PSYCHO_COOK_DURATION_TICKS = GameConstants.getInTicks(0, 40);

    /**
     * 厨师疯魔商店图标的购买冷却。
     */
    public static final int PSYCHO_COOK_COOLDOWN_TICKS = GameConstants.getInTicks(3, 45);

    /**
     * 厨师疯魔期间的心情下降倍率，0 表示完全不掉心情。
     */
    public static final float PSYCHO_COOK_MOOD_DRAIN_MULTIPLIER = 0.0F;

    /**
     * 厨师疯魔期间临时附加的体力上限，用足够大的值模拟“无限体力”。
     */
    public static final float PSYCHO_COOK_STAMINA_MAX_BONUS = 100000.0F;

    /**
     * 是否在厨师疯魔购买成功后给予夜视效果。
     * 默认开启；改为 false 时，厨师疯魔仍正常启动，但不会再额外获得夜视。
     */
    public static final boolean PSYCHO_COOK_GRANTS_NIGHT_VISION = true;

    /**
     * 厨师疯魔购买成功后给予的夜视效果等级，0 表示原版夜视 I。
     */
    public static final int PSYCHO_COOK_NIGHT_VISION_AMPLIFIER = 0;

    /**
     * 厨师疯魔 HUD 跑马文字颜色，用户指定为白色。
     */
    public static final int PSYCHO_COOK_TEXT_COLOR = 0xFFFFFF;

    /**
     * 厨师疯魔倒计时条颜色，和厨师/好人阵营心情条绿色保持一致。
     */
    public static final int PSYCHO_COOK_TIMER_BAR_COLOR = ROLE_COLOR;

    private CookConstants() {
    }
}
