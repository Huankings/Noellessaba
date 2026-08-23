package org.agmas.noellesroles.roles.spring_trap;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 弹簧陷阱所有可调玩法数值。
 *
 * <p>这个职业同时涉及商店、疯魔状态、投掷物光环和近战武器。
 * 后续平衡时只改这里，避免把 5 秒、40 秒、半径 5 格这类数字散落到不同 handler 里。</p>
 */
public final class SpringTrapConstants {
    /**
     * 弹簧陷阱职业色，按需求使用 RGB(110, 140, 20)。
     * 这个颜色会用于职业注册、疯魔文字颜色、倒计时条颜色等视觉表现。
     */
    public static final int ROLE_COLOR = 0x6E8C14;

    /**
     * 弹簧陷阱在 Harpy 角色分配中的最大生成数量。
     * 当前固定为 1，避免同局出现多个弹簧陷阱导致疯魔状态、光环和商店强度叠加失控。
     */
    public static final int MAX_ROLE_COUNT = 1;

    /**
     * 血斧右键蓄力暗杀所需的最低使用时间。
     * 5 tick = 0.25 秒；客户端松开右键后会按这个值判断是否允许发送暗杀请求，服务端也会再次校验。
     */
    public static final int BLOOD_AXE_MIN_USE_TICKS = GameConstants.getInTicks(0, 0) + 5;

    /**
     * 血斧右键使用动作的最大持续 tick。
     * 使用较大的原版常用值，让玩家可以一直保持蓄力姿态，直到松开右键触发检测。
     */
    public static final int BLOOD_AXE_MAX_USE_TICKS = 72000;

    /**
     * 血斧成功发动暗杀后的冷却时间。
     * 当前为 45 秒；只影响右键暗杀，左键击退不使用这个冷却。
     */
    public static final int BLOOD_AXE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 35);

    /**
     * 弹簧陷阱开局后血斧需要等待的初始冷却时间。
     * 这个冷却会在职业分配时写入 ItemCooldownManager，即使玩家稍后才购买血斧，也会从开局开始自然倒计时。
     */
    public static final int BLOOD_AXE_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    /**
     * 血斧右键暗杀的目标搜索距离。
     * 玩家准心需要在这个距离内锁到合法玩家，服务端才会允许用 axe 死因执行击杀。
     */
    public static final float BLOOD_AXE_TARGET_RANGE = 3.0F;

    /**
     * 血斧左键击退玩家时的水平击退强度。
     * 数值越大，被命中的玩家会被推得越远；这个动作无冷却。
     */
    public static final double BLOOD_AXE_KNOCKBACK_STRENGTH = 1.4D;

    /**
     * 血斧左键击退玩家时附加的向上速度。
     * 用来让击退手感更明显，避免目标只贴地水平滑动。
     */
    public static final double BLOOD_AXE_KNOCKBACK_UPWARD = 0.25D;

    /**
     * 彩虹斧左键秒杀的目标搜索距离。
     * 彩虹斧没有攻击冷却，因此这里控制它必须足够靠近并对准玩家才能连续击杀。
     */
    public static final float COLORFUL_AXE_TARGET_RANGE = 3.2F;

    /**
     * 彩虹斧客户端误触或服务端拒绝时给的极短临时冷却。
     * 这个冷却不是玩法限制，只是防止无目标时客户端连续刷包或疯狂触发失败反馈。
     */
    public static final int COLORFUL_AXE_TEMPORARY_FAIL_COOLDOWN_TICKS = 2;

    /**
     * 增速飞斧落地光环的最终半径，单位为方块。
     * 光环完全展开后会在这个半径内刷新增速或缓慢效果。
     */
    public static final int THROWING_SPEED_AURA_RADIUS_BLOCKS = 5;

    /**
     * 增速飞斧光环从中心向外展开到最大半径所需时间。
     * 当前为 2 秒；世界组件会记录 aura 年龄，因此时停者回溯时展开进度也会被倒回。
     */
    public static final int THROWING_SPEED_AURA_EXPAND_TICKS = GameConstants.getInTicks(0, 2);

    /**
     * 增速飞斧光环完全展开后继续维持的时间。
     * 当前为 7 秒；展开结束后仍然按最大半径持续刷粒子和药水效果。
     */
    public static final int THROWING_SPEED_AURA_FULL_TICKS = GameConstants.getInTicks(0, 7);

    /**
     * 增速飞斧光环从生成到消失的总持续时间。
     * 由展开时间加完全展开后的维持时间组成，避免后续改一个阶段后忘记同步总时长。
     */
    public static final int THROWING_SPEED_AURA_TOTAL_TICKS = THROWING_SPEED_AURA_EXPAND_TICKS + THROWING_SPEED_AURA_FULL_TICKS;

    /**
     * 增速飞斧光环刷新范围内玩家药水效果的间隔。
     * 当前每 20 tick 刷新一次，配合 4 秒药水持续时间，玩家离开光环后仍会保留短暂效果。
     */
    public static final int THROWING_SPEED_AURA_EFFECT_REFRESH_TICKS = 20;

    /**
     * 增速飞斧光环给友方玩家施加速度效果的持续时间。
     * 友方包括投掷者本人、杀手阵营，以及 Noelles 定义的杀手侧中立。
     */
    public static final int THROWING_SPEED_AURA_ALLY_SPEED_TICKS = GameConstants.getInTicks(0, 4);

    /**
     * 增速飞斧光环给敌方玩家施加缓慢效果的持续时间。
     * 敌方包括平民、义警和独立中立；普通中立不会被这个光环影响。
     */
    public static final int THROWING_SPEED_AURA_ENEMY_SLOWNESS_TICKS = GameConstants.getInTicks(0, 4);

    /**
     * 增速飞斧光环药水效果等级的放大器。
     * Minecraft 药水 amplifier 从 0 开始计数，所以 1 代表速度 II / 缓慢 II。
     */
    public static final int THROWING_SPEED_AURA_EFFECT_AMPLIFIER = 1;

    /**
     * 增速飞斧蓄力疾跑加速的移动修正优先级。
     * 数值越高越晚参与 Wathe 移动 API 的合并，用来保证这个职业效果能稳定覆盖普通移动调整。
     */
    public static final int THROWING_SPEED_AXE_CHARGE_SPEED_PRIORITY = 650;

    /**
     * 玩家疾跑并蓄力增速飞斧时的速度倍率。
     * 当前为 1.5 倍，只在手持增速飞斧、处于使用蓄力动作且正在疾跑时生效。
     */
    public static final float THROWING_SPEED_AXE_CHARGE_SPEED_MULTIPLIER = 1.5F;

    /**
     * 爆炸飞斧落地爆炸的判定半径。
     * 飞斧落地后会立刻按这个半径查找玩家，并使用 Wathe 手雷爆炸死因与回放逻辑击杀。
     */
    public static final float THROWING_BOMB_AXE_EXPLOSION_RADIUS = 3.0F;

    /**
     * 弹簧陷阱疯魔状态的基础持续时间。
     * 商店购买弹簧陷阱图标后启动该状态，状态期间锁定彩虹斧并启用专属皮肤、HUD 和环境音。
     */
    public static final int SPRING_TRAP_DURATION_TICKS = GameConstants.getInTicks(0, 45);

    /**
     * 弹簧陷阱续时器每次购买增加的疯魔状态时间。
     * 只能在已经处于弹簧陷阱状态且续时后不会超过基础最大持续时间时购买成功。
     */
    public static final int SPRING_TRAP_ADD_TIME_TICKS = GameConstants.getInTicks(0, 5);

    /**
     * 弹簧陷阱商店主动技能的购买冷却。
     * 当前为 4 分 30 秒；用于限制再次购买并启动弹簧陷阱状态的频率。
     */
    public static final int SPRING_TRAP_COOLDOWN_TICKS = GameConstants.getInTicks(4, 30);

    /**
     * 弹簧陷阱状态启动时显示给 Wathe 疯魔护盾系统的护盾层数。
     * 当前为 1 层；第一次受击后 HUD 会进入破损表现，但自定义护盾规则仍会继续拦截非坠落列车死亡。
     */
    public static final int SPRING_TRAP_SHIELD_LAYERS = 1;

    /**
     * 弹簧陷阱状态下被非坠落列车伤害命中后定身的时间。
     * 定身期间不能移动和跳跃，开始与解除都会记录专属回放事件。
     */
    public static final int SPRING_TRAP_ROOT_TICKS = GameConstants.getInTicks(0, 5);

    /**
     * 弹簧陷阱商店主动技能相对原疯魔模式条目的额外加价。
     * 最终价格 = Wathe 原疯魔模式价格 + 这个加价。
     */
    public static final int SPRING_TRAP_SHOP_PRICE_BONUS = 65;

    /**
     * 弹簧陷阱续时器的固定商店价格。
     * 这是一个购买即触发的工具型条目，不会真正把图标物品发给玩家。
     */
    public static final int SPRING_TRAP_ADDTIME_PRICE = 135;

    /**
     * 爆炸飞斧相对 Wathe 手雷条目的额外加价。
     * 最终价格 = 原手雷价格 + 这个加价，用来体现它兼具飞斧贯穿和落地爆炸。
     */
    public static final int THROWING_BOMB_AXE_GRENADE_PRICE_BONUS = 15;

    private SpringTrapConstants() {
    }
}
