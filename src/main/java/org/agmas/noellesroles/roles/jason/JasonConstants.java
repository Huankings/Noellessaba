package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 杰森职业全部可调玩法数值。
 *
 * <p>本职业同时涉及商店、投掷物、倒地救治、油桶燃烧、疯魔模式和客户端提示。
 * 为了后续平衡时不用在多个 handler 里翻数字，除职业 RGB 之外的所有数值都集中在这里。</p>
 */
public final class JasonConstants {
    /**
     * 杰森职业色，按需求使用 RGB(75, 30, 120)。
     * 该颜色用于职业注册、救治提示、本能透视与疯魔 HUD 文本。
     */
    public static final int ROLE_COLOR = (75 << 16) | (30 << 8) | 120;

    /**
     * 杰森在 Harpy 随机分配中的默认最大生成数。
     * 用户确认默认只允许一名杰森，避免多名杰森叠加倒地/油桶/疯魔压制强度。
     */
    public static final int MAX_ROLE_COUNT = 1;

    /**
     * 杰森投掷武器允许松手发射的最低蓄力时间。
     * 14 tick = 0.7 秒，比 NoellesRoles 旧飞斧更短，符合需求里的“蓄力更快”。
     */
    public static final int THROW_MIN_CHARGE_TICKS = 14;

    /**
     * 投掷武器右键使用动作的最大持续时间。
     * 使用原版弓/飞斧常见的大值，让玩家可以一直保持蓄力姿态直到松手。
     */
    public static final int THROW_MAX_USE_TICKS = 72000;

    /**
     * 普通杰森投掷武器的最低发射速度。
     * 低蓄力刚满足 0.7 秒时也能稳定飞出，但飞行距离会明显短于满蓄力。
     */
    public static final float THROW_VELOCITY_BASE = 0.9F;

    /**
     * 普通杰森投掷武器的满蓄力额外速度。
     * 与基础速度相加后高于旧飞斧，让它飞得更快更远。
     */
    public static final float THROW_VELOCITY_BONUS = 2.7F;

    /**
     * 投掷油桶的最低发射速度。
     * 油桶按需求比飞斧更慢，方便较快落地触发汽油范围。
     */
    public static final float JERRY_CAN_VELOCITY_BASE = 0.35F;

    /**
     * 投掷油桶的满蓄力额外速度。
     * 该值低于普通投掷武器，后续如果觉得油桶太难命中落点可优先调这里。
     */
    public static final float JERRY_CAN_VELOCITY_BONUS = 1.15F;

    /**
     * 投掷实体的随机散布。
     * 值越小越接近准星中心；杰森投掷武器偏向精准，因此低于旧飞斧的 1.0。
     */
    public static final float PROJECTILE_DIVERGENCE = 0.6F;

    /**
     * 投掷实体路径扫描时额外扩张的搜索盒半径。
     * 该值只负责先找候选玩家，真正命中仍会做玩家命中盒射线检测。
     */
    public static final double HIT_SCAN_BOX_EXPAND = 1.8D;

    /**
     * 玩家命中盒额外扩张值。
     * 高速飞行物如果完全按原版玩家盒判定会很容易穿模漏判，这里给少量容错。
     */
    public static final double PLAYER_HITBOX_EXPAND = 0.35D;

    /**
     * 普通投掷武器和飞镐的重力值。
     * 低于油桶，让蓄力后飞行距离更长。
     */
    public static final double THROWING_WEAPON_GRAVITY = 0.035D;

    /**
     * 投掷油桶的重力值。
     * 高于普通投掷武器，使其更容易落地并展开汽油/火焰机制。
     */
    public static final double JERRY_CAN_GRAVITY = 0.085D;

    /**
     * 普通投掷实体最长存在时间。
     * 用于兜底清理飞行中或插在方块上的实体，防止残局中永久残留。
     */
    public static final int DEFAULT_PROJECTILE_LIFETIME_TICKS = GameConstants.getInTicks(2, 0);

    /**
     * 杰森模式飞镐落地后的实体保留时间。
     * 用户指定飞镐落地后只存在 15 秒，时间到自动清理。
     */
    public static final int PICKAXE_STUCK_LIFETIME_TICKS = GameConstants.getInTicks(0, 15);

    /**
     * 重伤倒地无人救治时的失血倒计时。
     * 当前为 25 秒；救治进行中会暂停该倒计时。
     */
    public static final int BLEED_OUT_TICKS = GameConstants.getInTicks(0, 25);

    /**
     * 第一次重伤倒地的基础救治时间。
     * 当前为 10 秒；手持医疗箱时会按医疗箱倍率缩短。
     */
    public static final int FIRST_RESCUE_TICKS = GameConstants.getInTicks(0, 10);

    /**
     * 第二次重伤倒地的基础救治时间。
     * 当前为 20 秒；第三次再被穿刺时不再倒地，直接死亡。
     */
    public static final int SECOND_RESCUE_TICKS = GameConstants.getInTicks(0, 20);

    /**
     * 手持 noellesroles 医疗箱救治时的时间倍率。
     * 0.5 表示救治时间减半；具体所需 tick 会按当前倒地次数动态计算。
     */
    public static final double MEDICAL_KIT_RESCUE_TIME_MULTIPLIER = 0.5D;

    /**
     * 被救起后给予黑暗和缓慢效果的持续时间。
     * 当前为 5 秒，制造刚脱离濒死后的短暂虚弱窗口。
     */
    public static final int RESCUED_EFFECT_TICKS = GameConstants.getInTicks(0, 5);

    /**
     * 被救起后缓慢效果的等级放大器。
     * Minecraft amplifier 从 0 开始计数，所以 1 代表缓慢 II。
     */
    public static final int RESCUED_SLOWNESS_AMPLIFIER = 1;

    /**
     * 玩家准心对准倒地玩家并蹲下救治的最大距离。
     * Wathe 存活玩家准心名字默认是 2 格，这里略放宽一点，避免边缘距离因同步误差中断。
     */
    public static final double RESCUE_RANGE_BLOCKS = 2.35D;

    /**
     * 服务端判断“准心对准倒地玩家”的角度余弦阈值。
     * 越接近 1 要求越精确；0.965 大约是 15 度以内。
     */
    public static final double RESCUE_LOOK_DOT_MIN = 0.965D;

    /**
     * 重伤倒地时强制覆盖的移动速度。
     * 该值保留低速爬行能力，但明显低于 Wathe 普通步行速度。
     */
    public static final float WOUNDED_CRAWL_SPEED = 0.018F;

    /**
     * 重伤倒地移动修正的优先级。
     * 使用较高优先级覆盖多数职业加速，确保倒地状态下不能靠其它速度机制起身乱跑。
     */
    public static final int WOUNDED_MOVEMENT_PRIORITY = 12000;

    /**
     * 投掷武器每次把玩家打入重伤倒地时，投掷者获得的金币。
     * 用户指定为 60 金币。
     */
    public static final int WOUND_REWARD_COINS = 60;

    /**
     * 第三次穿刺直接击杀时，在默认击杀奖励之外额外发给杰森的金币。
     * 用户指定为每人额外 40 金币。
     */
    public static final int DIRECT_KILL_EXTRA_REWARD_COINS = 40;

    /**
     * 油桶落地后给玩家沾染汽油的半径。
     * 用户指定为 3 格。
     */
    public static final double JERRY_CAN_GASOLINE_RADIUS = 3.0D;

    /**
     * 油桶落地后自动燃烧前的延迟。
     * 用户指定为 4 秒；一次性打火机可以在此之前主动点燃。
     */
    public static final int JERRY_CAN_AUTO_IGNITE_TICKS = GameConstants.getInTicks(0, 4);

    /**
     * 火焰粒子效果的最终半径。
     * 用户指定为 5 格；进入该范围的存活玩家会立即以 burn 死因致死。
     */
    public static final double FIRE_RADIUS_BLOCKS = 5.0D;

    /**
     * 火焰范围从中心扩张到最大半径的过渡时间。
     * 用户指定为 2 秒，视觉粒子会按该进度向外扩散。
     */
    public static final int FIRE_EXPAND_TICKS = GameConstants.getInTicks(0, 2);

    /**
     * 火焰范围完全存在的总持续时间。
     * 用户指定为 6 秒；到时后世界组件会移除该火焰记录。
     */
    public static final int FIRE_DURATION_TICKS = GameConstants.getInTicks(0, 6);

    /**
     * 火焰范围每 tick 生成的粒子数量。
     * 这是视觉密度，不影响真实致死判定范围。
     */
    public static final int FIRE_PARTICLES_PER_TICK = 42;

    /**
     * 油桶和火焰范围的垂直判定高度。
     * 玩家站在楼上/楼下时不会因为水平距离够近而无限跨层被烧。
     */
    public static final double FIRE_VERTICAL_RANGE = 2.4D;

    /**
     * 被油桶沾染汽油的玩家在杰森视角下的被动透视颜色。
     * 用户指定为橙色，这里使用常见高可读橙色 0xFF8A00。
     */
    public static final int GASOLINE_INSTINCT_COLOR = 0xFF8A00;

    /**
     * 重伤救治准心提示的缩放比例。
     * 该提示复用 Wathe 名牌 HUD 的 0.6 缩放，避免长中文文本遮住画面中心。
     */
    public static final float ROLE_NAME_HUD_SCALE = 0.6F;

    /**
     * 重伤救治准心提示在名牌 HUD 坐标系中的纵向偏移。
     * 该位置刻意低于玩家名与可能出现的杀手同伙提示，减少和其它职业提示重叠。
     */
    public static final int ROLE_NAME_HUD_Y_OFFSET = 42;

    /**
     * 倒地本人濒死 HUD 的基础缩放比例。
     * 1.0 表示接近原版 actionbar 文本大小；极窄窗口下会按可用宽度自动缩小。
     */
    public static final float WOUNDED_SELF_HUD_SCALE = 1.0F;

    /**
     * 倒地本人濒死 HUD 距离屏幕底部的像素距离。
     * 原版 actionbar 大约位于底部热键栏上方，这里略微靠下单独绘制，避免和 actionbar 文本互相挤占。
     */
    public static final int WOUNDED_SELF_HUD_Y_FROM_BOTTOM = 56;

    /**
     * 倒地本人濒死 HUD 在窄屏时保留的左右安全边距。
     * 文本过长时会用该边距计算缩放，防止长中文提示超出屏幕。
     */
    public static final int WOUNDED_SELF_HUD_HORIZONTAL_PADDING = 12;

    /**
     * 杰森模式持续时间。
     * 用户指定为 45 秒。
     */
    public static final int PSYCHO_DURATION_TICKS = GameConstants.getInTicks(0, 45);

    /**
     * 杰森模式商店图标的购买冷却。
     * 用户指定为 4 分 15 秒。
     */
    public static final int PSYCHO_COOLDOWN_TICKS = GameConstants.getInTicks(4, 15);

    /**
     * 随机投掷武器商店价格。
     * 用户指定为 165 金币，购买后随机给四种普通投掷武器之一。
     */
    public static final int RANDOM_THROWING_WEAPON_PRICE = 165;

    /**
     * 投掷油桶商店价格。
     * 用户指定为 275 金币。
     */
    public static final int THROWING_JERRY_CAN_PRICE = 275;

    /**
     * 杰森模式相对 Wathe 默认疯魔模式的额外加价。
     * 用户指定比原疯魔模式贵 25 金币。
     */
    public static final int PSYCHO_PRICE_BONUS = 25;

    /**
     * 无恶不在开局冷却。
     * 用户指定为 40 秒；该冷却属于“开局保护期”，杰森击杀不会把它清零。
     */
    public static final int ABILITY_INITIAL_COOLDOWN_TICKS = GameConstants.getInTicks(0, 40);

    /**
     * 无恶不在正常解除后的冷却。
     * 用户指定为 15 秒；只有该冷却允许在杰森确认击杀后被立即清零。
     */
    public static final int ABILITY_EXIT_COOLDOWN_TICKS = GameConstants.getInTicks(0, 15);

    /**
     * 无恶不在发动时迷雾笼罩和隐身进入的过渡时间。
     * 用户指定为 2 秒，客户端雾效会按该时间从普通视距平滑压到目标视距。
     */
    public static final int ABILITY_ENTER_TICKS = GameConstants.getInTicks(0, 2);

    /**
     * 无恶不在主动解除时迷雾消散和显形的过渡时间。
     * 用户指定为 2 秒，完全解除后才触发范围惊吓。
     */
    public static final int ABILITY_EXIT_TICKS = GameConstants.getInTicks(0, 2);

    /**
     * 无恶不在发动后允许主动解除前必须等待的最短时间。
     * 用户指定为 7 秒；计时从按下能力键成功发动那一刻开始，包含 2 秒进入过渡。
     */
    public static final int ABILITY_MIN_TICKS_BEFORE_EXIT = GameConstants.getInTicks(0, 7);

    /**
     * 无恶不在状态下杰森的移动速度倍率。
     * 用户指定为原速度的 2.5 倍；该倍率通过 Wathe PlayerMovementApi 叠加。
     */
    public static final float ABILITY_SPEED_MULTIPLIER = 2.5F;

    /**
     * 无恶不在速度修正优先级。
     * 低于重伤倒地覆盖优先级，保证极端调试状态下“倒地不能乱跑”仍然优先。
     */
    public static final int ABILITY_MOVEMENT_PRIORITY = 2500;

    /**
     * 存活玩家视角下无恶不在迷雾的起点。
     * 用户指定为 2.0，制造杰森幽魂压迫下的极近可视距离。
     */
    public static final float ABILITY_FOG_SURVIVAL_START = 2.0F;

    /**
     * 存活玩家视角下无恶不在迷雾的终点。
     * 用户指定为 4.0，存活玩家只能看到很短距离内的环境。
     */
    public static final float ABILITY_FOG_SURVIVAL_END = 4.0F;

    /**
     * 杰森本人视角下无恶不在迷雾的起点。
     * 用户指定为 3.0，让杰森自己也受迷雾限制，但略宽于普通存活玩家。
     */
    public static final float ABILITY_FOG_JASON_SELF_START = 3.0F;

    /**
     * 杰森本人视角下无恶不在迷雾的终点。
     * 用户指定为 7.0，配合红色粒子提示追踪存活玩家动静。
     */
    public static final float ABILITY_FOG_JASON_SELF_END = 7.0F;

    /**
     * 非存活 / 创造 / 旁观调试视角下无恶不在迷雾的起点。
     * 用户指定为 5.0，避免复盘和管理视角被压得过窄。
     */
    public static final float ABILITY_FOG_NON_SURVIVAL_START = 5.0F;

    /**
     * 非存活 / 创造 / 旁观调试视角下无恶不在迷雾的终点。
     * 用户指定为 10.0，仍能看出场上处于无恶不在状态，但不会影响调试观察。
     */
    public static final float ABILITY_FOG_NON_SURVIVAL_END = 10.0F;

    /** 无恶不在雾效的三类观看者开关，使用 Boolean 方便后续调试反射读取。 */
    public static final Boolean ABILITY_FOG_FOR_JASON_SELF = false;
    public static final Boolean ABILITY_FOG_FOR_OTHER_SURVIVORS = false;
    public static final Boolean ABILITY_FOG_FOR_NON_SURVIVAL = false;

    /** 无恶不在失明效果的三类目标开关。 */
    public static final Boolean ABILITY_BLINDNESS_FOR_JASON_SELF = true;
    public static final Boolean ABILITY_BLINDNESS_FOR_OTHER_SURVIVORS = false;
    public static final Boolean ABILITY_BLINDNESS_FOR_NON_SURVIVAL = false;

    /** 杰森失明归属每 tick 刷新的短效果时长，和 Wathe 停电药水保持一致。 */
    public static final int ABILITY_BLINDNESS_REFRESH_TICKS = 60;
    /** 自然结束时允许识别为杰森短效果的最大剩余时间。 */
    public static final int ABILITY_BLINDNESS_MAX_OWNED_DURATION_TICKS = ABILITY_BLINDNESS_REFRESH_TICKS + 10;

    /**
     * 无恶不在雾效 provider 的优先级。
     * 数值高于普通地图/默认雾 provider，确保杰森能力的最终雾距不会被其它扩展覆盖。
     */
    public static final int ABILITY_FOG_PRIORITY = 25000;

    /**
     * 无恶不在完全解除后的惊吓半径。
     * 用户指定为 4 格，只影响解除瞬间范围内的存活玩家。
     */
    public static final double ABILITY_SCARE_RADIUS_BLOCKS = 4.0D;

    /**
     * 惊吓状态持续时间。
     * 用户指定为 4 秒，期间心情下降倍率提高并获得黑暗效果。
     */
    public static final int ABILITY_SCARE_TICKS = GameConstants.getInTicks(0, 4);

    /**
     * 惊吓状态下掉 san / 心情下降速度倍率。
     * 用户指定为原来的 4 倍；该倍率会乘在当前职业已有心情倍率之后。
     */
    public static final float ABILITY_SCARE_MOOD_DRAIN_MULTIPLIER = 4.0F;

    /**
     * 惊吓状态附加黑暗效果的等级放大器。
     * 黑暗没有等级语义，保留为 0 方便后续若改成别的效果时统一调参。
     */
    public static final int ABILITY_SCARE_DARKNESS_AMPLIFIER = 0;

    /**
     * 存活玩家原地不动多久后会在杰森视角下暴露红色粒子。
     * 用户指定为 0.5 秒，即 10 tick。
     */
    public static final int ABILITY_STATIONARY_REVEAL_TICKS = GameConstants.getInTicks(0, 0) + 10;

    /**
     * 判断“玩家是否正在移动”的最小水平位移平方。
     * 该值用于过滤服务端同步和浮点抖动，避免真正静止的玩家每 tick 都被当作移动。
     */
    public static final double ABILITY_REVEAL_MOVEMENT_EPSILON_SQUARED = 0.0009D;

    /**
     * 红色提示粒子每次刷新时的数量。
     * 这是纯表现数值，只发给杰森本人，不会向其他玩家暴露目标位置。
     */
    public static final int ABILITY_REVEAL_PARTICLE_COUNT = 9;

    /**
     * 红色提示粒子的显示刷新间隔。
     * 每 5 tick 刷一次能保持显眼，同时避免每 tick 大量定向粒子造成网络压力。
     */
    public static final int ABILITY_REVEAL_PARTICLE_INTERVAL_TICKS = 5;

    /**
     * 红色提示粒子的水平扩散范围。
     * 值越大，粒子云越宽；这里控制在玩家身体附近，方便杰森判断大致位置。
     */
    public static final double ABILITY_REVEAL_PARTICLE_HORIZONTAL_SPREAD = 0.35D;

    /**
     * 红色提示粒子的竖直扩散范围。
     * 覆盖玩家身体高度，让提示比单点更明显。
     */
    public static final double ABILITY_REVEAL_PARTICLE_VERTICAL_SPREAD = 0.9D;

    /**
     * 红色提示粒子的尺寸。
     * Dust 粒子尺寸略大于默认值，保证迷雾内可读。
     */
    public static final float ABILITY_REVEAL_PARTICLE_SCALE = 1.35F;

    /**
     * 无恶不在目标可见性规则优先级。
     * 使用较高优先级先于大部分职业目标规则，保证幽魂状态不会被其它允许规则重新暴露。
     */
    public static final int ABILITY_TARGET_VISIBILITY_PRIORITY = 25000;

    /**
     * 无恶不在期间，是否让杰森之外的其他存活玩家彼此完全不可见。
     *
     * <p>开启时会通过 TargetVisibilityApi 同时禁止玩家实体渲染、准心选中、
     * 右键交互和攻击；关闭时只恢复其他存活玩家彼此之间的目标关系，
     * 不影响“存活玩家看不到幽魂杰森”和“幽魂杰森看不到存活玩家”的原有规则。</p>
     */
    public static final Boolean ABILITY_HIDE_OTHER_SURVIVORS_FROM_EACH_OTHER = false;

    /**
     * 无恶不在玩家碰撞规则优先级。
     * 高于 Wathe 默认玩家实体墙，确保幽魂杰森能穿过存活玩家。
     */
    public static final int ABILITY_COLLISION_PRIORITY = 25000;

    /**
     * 无恶不在期间，是否取消杰森之外的其他存活玩家彼此之间的碰撞与推挤。
     *
     * <p>开启时返回 PlayerCollisionMode.NO_COLLISION，同时取消 Wathe 玩家实体墙和
     * 原版轻微推挤；关闭时其他存活玩家恢复当前服务器的默认碰撞规则，
     * 但幽魂杰森与其他玩家之间仍然保持无碰撞、无推挤。</p>
     */
    public static final Boolean ABILITY_DISABLE_OTHER_SURVIVOR_COLLISION = false;

    /**
     * 无恶不在手持物隐藏规则优先级。
     * 高于普通职业专属物品隐藏，保证任何手持物都对其他存活玩家不可见。
     */
    public static final int ABILITY_HELD_ITEM_VISIBILITY_PRIORITY = 25000;

    /**
     * 无恶不在本能透视压制优先级。
     * 高于时间狭缝以外的大部分本能颜色规则，确保杀手本能和被动透视都不能看见幽魂杰森。
     */
    public static final int ABILITY_INSTINCT_SUPPRESSION_PRIORITY = 31000;

    /**
     * 无恶不在音效默认音量。
     * 播放给指定玩家时使用 MASTER 分类和该音量，确保全局感足够明显。
     */
    public static final float ABILITY_SOUND_VOLUME = 1.5F;

    /**
     * 无恶不在音效默认音高。
     * 保留为常量方便后续按素材响度统一微调。
     */
    public static final float ABILITY_SOUND_PITCH = 1.0F;

    /**
     * 无恶不在持续环境音循环检查间隔。
     * 与 Wathe 疯魔环境音一致，每 20 tick 检查一次是否需要继续播放。
     */
    public static final int ABILITY_LOOP_SOUND_INTERVAL_TICKS = 20;

    /**
     * 无恶不在持续环境音最开始播放时的淡入时间。
     * 用户要求最开始播放的持续音有淡入效果；这里默认 2 秒，后续可按素材响度调整。
     */
    public static final int ABILITY_LOOP_SOUND_FADE_IN_TICKS = GameConstants.getInTicks(0, 2);

    /**
     * 无恶不在持续环境音退出时的淡出时间。
     * 用户要求主动退出或死亡被动退出时，持续音淡出长度和显形过渡完全一致，因此这里直接跟随退出过渡常量。
     */
    public static final int ABILITY_LOOP_SOUND_FADE_OUT_TICKS = ABILITY_EXIT_TICKS;

    /**
     * 无恶不在期间，非杰森存活玩家的 proximity voice 传播距离倍率。
     * 服务端会先把原语音距离乘以该倍率，让声音从距离层面更快衰减。
     */
    public static final float ABILITY_VOICE_DISTANCE_MULTIPLIER = 0.28F;

    /**
     * 无恶不在期间，非杰森存活玩家的 proximity voice 最大传播距离。
     * 即使服务器原本语音距离很远，也会被压到该上限，方便后续单独调整“最远能听多远”。
     */
    public static final float ABILITY_VOICE_MAX_DISTANCE_BLOCKS = 8.0F;

    /**
     * 无恶不在期间，非杰森存活玩家互相听到语音时的基础音量倍率。
     * 该值在客户端接收音频时压低 PCM 样本，弥补服务端距离缩短无法改变近距离初始音量的问题。
     */
    public static final float ABILITY_VOICE_VOLUME_MULTIPLIER = 0.45F;

    /**
     * 无恶不在手臂完全收起时的最小缩放。
     * 使用很小但非 0 的值，避免第一人称矩阵出现完全 0 缩放导致法线或部分渲染管线异常。
     */
    public static final float ABILITY_ARM_HIDDEN_SCALE = 0.02F;

    /**
     * 无恶不在手臂收起时向屏幕下方移出的距离。
     * 第一人称手臂默认也在负 Y 方向，因此继续向负 Y 推会表现为手臂滑出屏幕下沿。
     */
    public static final float ABILITY_ARM_HIDE_TRANSLATE_Y = -1.15F;

    /**
     * 无恶不在手臂收起时向镜头远处移出的距离。
     * 负 Z 会把第一人称手臂 / 手持物推远，配合缩放保证完全进入幽魂状态后不可见。
     */
    public static final float ABILITY_ARM_HIDE_TRANSLATE_Z = -0.35F;

    private JasonConstants() {
    }
}
