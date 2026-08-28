package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 巫妖职业的所有玩法数值。
 *
 * <p>除职业 RGB 外，用户要求涉及到的数值都集中到这里。
 * 后续调平衡时优先改本类，业务代码只表达机制，不把魔法数字散在各处。</p>
 */
public final class LichConstants {
    /** 巫妖职业颜色，格式为 0xRRGGBB，对应用户指定 RGB(200,50,110)。 */
    public static final int ROLE_COLOR = 0xC8326E;

    /** 巫妖在 Harpy 随机分配池中的最大生成数量。 */
    public static final int MAX_ROLE_COUNT = 1;

    /** 简易法杖最短蓄力时间；达到 0.6 秒后松开才会真正发射法术骷髅。 */
    public static final int ONCE_STAFF_MIN_CHARGE_TICKS = GameConstants.getInTicks(0, 0) + 12;

    /** 简易法杖的最大持续使用时间；沿用弓类长按结构，真实释放只看最短蓄力。 */
    public static final int ONCE_STAFF_MAX_USE_TICKS = 72000;

    /** 简易法杖释放后的物品冷却时间，正式玩家会进入 10 秒冷却。 */
    public static final int ONCE_STAFF_COOLDOWN_TICKS = GameConstants.getInTicks(0, 10);

    /** 简易法杖每次释放的法术骷髅数量。 */
    public static final int ONCE_STAFF_SKELETON_COUNT = 5;

    /** 简易法杖骷髅的水平扇形角度，单位为度。 */
    public static final float ONCE_STAFF_FAN_DEGREES = 40.0F;

    /** 简易法杖骷髅最长飞行距离，达到 80 格后自动清理实体。 */
    public static final double ONCE_STAFF_RANGE_BLOCKS = 80.0D;

    /** 简易法杖骷髅飞行速度，单位为格/游戏刻；用户确认先按建议值 9.5。 */
    public static final float ONCE_STAFF_SKULL_SPEED_BLOCKS_PER_TICK = 9.5F/20.0F;

    /** 法杖骷髅生成位置相对施法者眼睛高度的下移量，避免刚生成就挡住玩家视线。 */
    public static final double SKELETON_SPAWN_EYE_Y_OFFSET = -0.1D;

    /** 疯魔法杖右键释放后的右键物品冷却时间。 */
    public static final int PSYCHO_STAFF_COOLDOWN_TICKS = GameConstants.getInTicks(0, 2);

    /** 疯魔法杖右键每次释放的亡灵骷髅数量。 */
    public static final int PSYCHO_STAFF_SKELETON_COUNT = 8;

    /** 疯魔法杖骷髅的水平扇形角度，单位为度。 */
    public static final float PSYCHO_STAFF_FAN_DEGREES = 60.0F;

    /** 疯魔法杖骷髅最长飞行距离，达到 160 格后自动清理实体。 */
    public static final double PSYCHO_STAFF_RANGE_BLOCKS = 160.0D;

    /** 疯魔法杖骷髅飞行速度，单位为格/游戏刻；用户确认先按建议值 12.5。 */
    public static final float PSYCHO_STAFF_SKULL_SPEED_BLOCKS_PER_TICK = 12.5F/20.0F;

    /** 骷髅投射物命中判定的扫描包围盒扩张量，用于避免高速飞行穿过玩家。 */
    public static final double SKELETON_HIT_SCAN_BOX_EXPAND = 1.25D;

    /** 骷髅实体碰撞盒宽度，沿用凋零头的较小体积。 */
    public static final float SKELETON_ENTITY_WIDTH_BLOCKS = 0.3125F;

    /** 骷髅实体碰撞盒高度，沿用凋零头的较小体积。 */
    public static final float SKELETON_ENTITY_HEIGHT_BLOCKS = 0.3125F;

    /** 骷髅实体同步追踪范围，略大于简易法杖 80 格射程，保证远端玩家看得到飞行体。 */
    public static final int SKELETON_ENTITY_TRACKING_RANGE_BLOCKS = 96;

    /** 骷髅实体每 tick 同步一次，避免高速投射物在客户端出现明显跳帧。 */
    public static final int SKELETON_ENTITY_TRACKING_INTERVAL_TICKS = 1;

    /** 骷髅投射物对玩家碰撞盒额外扩张量，提升擦边命中的稳定性。 */
    public static final double SKELETON_PLAYER_HITBOX_EXPAND = 0.2D;

    /** 骷髅飞行逻辑不使用原版爆炸投射物额外加速度，只采用生成时写入的固定速度。 */
    public static final double SKELETON_ACCELERATION_POWER = 0.0D;

    /** 骷髅投射物无论是否命中都会存在的最大保险寿命，防止异常情况下实体残留。 */
    public static final int SKELETON_MAX_LIFETIME_TICKS = GameConstants.getInTicks(0, 15);

    /** 骷髅命中方块或到达距离后播放爆炸粒子的数量。 */
    public static final int SKELETON_IMPACT_PARTICLE_COUNT = 1;

    /** 骷髅发射音效音量。 */
    public static final float SKELETON_SHOOT_SOUND_VOLUME = 1.0F;

    /** 骷髅发射音效基础音高。 */
    public static final float SKELETON_SHOOT_SOUND_PITCH = 1.0F;

    /** 骷髅命中方块或玩家时的爆炸音效音量。 */
    public static final float SKELETON_IMPACT_SOUND_VOLUME = 1.0F;

    /** 骷髅命中方块或玩家时的爆炸音效音高。 */
    public static final float SKELETON_IMPACT_SOUND_PITCH = 1.0F;

    /** 骷髅爆炸粒子不需要随机散布，三个轴向偏移都固定为 0。 */
    public static final double SKELETON_IMPACT_PARTICLE_SPREAD = 0.0D;

    /** 骷髅爆炸粒子不需要额外速度，保持凋零命中那种原地爆开观感。 */
    public static final double SKELETON_IMPACT_PARTICLE_SPEED = 0.0D;

    /** 魔法屏障最短蓄力时间；达到 0.5 秒后松开才会生成屏障。 */
    public static final int MAGIC_BARRIER_MIN_CHARGE_TICKS = GameConstants.getInTicks(0, 0) + 10;

    /** 魔法屏障的最大持续使用时间；沿用弓类长按结构，真实释放只看最短蓄力。 */
    public static final int MAGIC_BARRIER_MAX_USE_TICKS = 72000;

    /** 魔法屏障物品冷却时间；屏障影响武器时也刷新到同样的 10 秒。 */
    public static final int MAGIC_BARRIER_ITEM_COOLDOWN_TICKS = GameConstants.getInTicks(0, 10);

    /** 魔法屏障从 0 扩大到最终半径所需时间，用户要求为 2 秒。 */
    public static final int MAGIC_BARRIER_EXPAND_TICKS = GameConstants.getInTicks(0, 2);

    /** 魔法屏障最终半径，单位为格。 */
    public static final double MAGIC_BARRIER_RADIUS_BLOCKS = 6.0D;

    /** 魔法屏障实体碰撞盒宽度；实体本身只负责同步位置，实际范围由半径常量控制。 */
    public static final float MAGIC_BARRIER_ENTITY_WIDTH_BLOCKS = 0.1F;

    /** 魔法屏障实体碰撞盒高度；实体本身只负责同步位置，实际范围由半径常量控制。 */
    public static final float MAGIC_BARRIER_ENTITY_HEIGHT_BLOCKS = 0.1F;

    /** 魔法屏障实体同步追踪范围，略大于 120 格飞行距离，保证远处玩家也能看见粒子来源。 */
    public static final int MAGIC_BARRIER_ENTITY_TRACKING_RANGE_BLOCKS = 128;

    /** 魔法屏障实体每 tick 同步一次，粒子球移动时客户端表现更稳定。 */
    public static final int MAGIC_BARRIER_ENTITY_TRACKING_INTERVAL_TICKS = 1;

    /** 魔法屏障生成位置相对施法者眼睛高度的下移量，让球体中心从胸口前方推出。 */
    public static final double MAGIC_BARRIER_SPAWN_EYE_Y_OFFSET = -0.2D;

    /** 魔法屏障飞行速度，单位为格/游戏刻；4.5 格/秒折算为 0.225 格/tick。 */
    public static final double MAGIC_BARRIER_SPEED_BLOCKS_PER_TICK = 4.5D / 20.0D;

    /** 魔法屏障最大飞行距离，达到 120 格后停下并消失。 */
    public static final double MAGIC_BARRIER_RANGE_BLOCKS = 120.0D;

    /** 魔法屏障刷新目标通用 AbilityPlayerComponent 的技能冷却时间。 */
    public static final int MAGIC_BARRIER_ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 15);

    /** 魔法屏障每 tick 采样球体粒子的经线数量。 */
    public static final int MAGIC_BARRIER_PARTICLE_LONGITUDE_STEPS = 18;

    /** 魔法屏障每 tick 采样球体粒子的纬线数量。 */
    public static final int MAGIC_BARRIER_PARTICLE_LATITUDE_STEPS = 9;

    /** 魔法屏障粒子的尺寸，DustParticleEffect 会用它控制单个粒子的可见大小。 */
    public static final float MAGIC_BARRIER_PARTICLE_SCALE = 1.15F;

    /** 魔法屏障粒子的红色通道，单独常量化以便之后不改实体代码就能换职业色。 */
    public static final float MAGIC_BARRIER_PARTICLE_RED = 200.0F / 255.0F;

    /** 魔法屏障粒子的绿色通道，单独常量化以便之后不改实体代码就能换职业色。 */
    public static final float MAGIC_BARRIER_PARTICLE_GREEN = 50.0F / 255.0F;

    /** 魔法屏障粒子的蓝色通道，单独常量化以便之后不改实体代码就能换职业色。 */
    public static final float MAGIC_BARRIER_PARTICLE_BLUE = 110.0F / 255.0F;

    /** 魔法屏障半径过小时不生成粒子，避免 0 半径阶段在玩家身上堆无意义粒子。 */
    public static final double MAGIC_BARRIER_MIN_VISIBLE_RADIUS_BLOCKS = 0.05D;

    /** 计算球面纬线/经线时的一整圈弧度倍率。 */
    public static final double MAGIC_BARRIER_FULL_CIRCLE_RADIANS_MULTIPLIER = 2.0D;

    /** 魔法屏障球壳粒子一次只生成一个点，粒子密度由经纬步数控制。 */
    public static final int MAGIC_BARRIER_PARTICLES_PER_POINT = 1;

    /** 魔法屏障粒子不需要随机散布，三个轴向偏移都固定为 0。 */
    public static final double MAGIC_BARRIER_PARTICLE_SPREAD = 0.0D;

    /** 魔法屏障粒子不需要额外速度，屏障位移由实体自身移动表现。 */
    public static final double MAGIC_BARRIER_PARTICLE_SPEED = 0.0D;

    /** 魔法屏障释放时的音效音量。 */
    public static final float MAGIC_BARRIER_CAST_SOUND_VOLUME = 1.0F;

    /** 魔法屏障释放时的音效音高。 */
    public static final float MAGIC_BARRIER_CAST_SOUND_PITCH = 1.1F;

    /** 巫妖控门术扫描半径，单位为格。 */
    public static final int DOOR_CONTROL_RADIUS_BLOCKS = 4;

    /** 巫妖控门术开局冷却时间。 */
    public static final int DOOR_CONTROL_START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);

    /** 巫妖控门术释放后的技能冷却时间。 */
    public static final int DOOR_CONTROL_USE_COOLDOWN_TICKS = GameConstants.getInTicks(2, 0);

    /** 控门术锁门持续时间，正常门会被刷新为 30 秒锁定。 */
    public static final int DOOR_CONTROL_JAM_TICKS = GameConstants.getInTicks(0, 30);

    /** 控门术播放门类音效时使用的音量。 */
    public static final float DOOR_CONTROL_SOUND_VOLUME = 1.0F;

    /** 控门术播放门类音效时使用的音高。 */
    public static final float DOOR_CONTROL_SOUND_PITCH = 1.0F;

    /** 门音效播放点的水平偏移，使声音从门中心而不是方块角落发出。 */
    public static final double DOOR_CONTROL_SOUND_CENTER_OFFSET = 0.5D;

    /** 门音效播放点的高度偏移，使声音更接近双格门的中部。 */
    public static final double DOOR_CONTROL_SOUND_Y_OFFSET = 1.0D;

    /** 控门术修复破门时用于清空 Wathe 门被破坏状态的计时值。 */
    public static final int DOOR_CONTROL_REPAIRED_TIMER_TICKS = 0;

    /** 控门术扫描和回放计数使用的“没有门/没有影响”基准值。 */
    public static final int DOOR_CONTROL_EMPTY_COUNT = 0;

    /** 商店价格最低不低于 0 金币，避免默认价格被其它模组改低后折扣算出负数。 */
    public static final int MIN_SHOP_PRICE = 0;

    /** 简易法杖商店价格相对 Wathe 默认左轮减少的金币数量。 */
    public static final int ONCE_STAFF_PRICE_DISCOUNT = 25;

    /** 读取不到 Wathe 左轮默认价格时的兜底金币价格，只用于商店 API 异常兜底。 */
    public static final int ONCE_STAFF_BASE_PRICE_FALLBACK = 250;

    /** 魔法屏障商店价格相对 Wathe 默认手雷减少的金币数量。 */
    public static final int MAGIC_BARRIER_PRICE_DISCOUNT = 20;

    /** 读取不到 Wathe 手雷默认价格时的兜底金币价格，只用于商店 API 异常兜底。 */
    public static final int MAGIC_BARRIER_BASE_PRICE_FALLBACK = 300;

    /** 巫妖疯魔商店图标的固定购买价格。 */
    public static final int PSYCHO_LICH_PRICE = 370;

    /** 巫妖疯魔商店图标的购买冷却时间，用户要求为 4 分 20 秒。 */
    public static final int PSYCHO_LICH_COOLDOWN_TICKS = GameConstants.getInTicks(4, 20);

    /** 巫妖疯魔持续时间，用户要求为 45 秒。 */
    public static final int PSYCHO_LICH_DURATION_TICKS = GameConstants.getInTicks(0, 45);

    /** 巫妖疯魔护盾数量，用户要求为 1。 */
    public static final int PSYCHO_LICH_SHIELD_COUNT = 1;

    /** 巫妖疯魔 HUD 文字颜色，按职业色显示。 */
    public static final int PSYCHO_LICH_TEXT_COLOR = ROLE_COLOR;

    /** 巫妖疯魔 HUD 倒计时条颜色，按职业色显示。 */
    public static final int PSYCHO_LICH_TIMER_BAR_COLOR = ROLE_COLOR;

    /** 巫妖疯魔 HUD 样式优先级增量，保证它覆盖 Wathe 默认疯魔 HUD。 */
    public static final int PSYCHO_LICH_MOOD_HUD_PRIORITY_BONUS = 100;

    /** 巫妖疯魔音乐淡入/淡出 tick 数，沿用现有疯魔音乐 20tick 的平滑过渡。 */
    public static final int PSYCHO_LICH_BACKGROUND_FADE_TICKS = 20;

    /** Minecraft 玩家空手基础攻击速度；物品属性要用“目标速度 - 基础速度”的差值写入。 */
    public static final double VANILLA_PLAYER_BASE_ATTACK_SPEED = 4.0D;

    /** 疯魔法杖主手攻击速度；Wathe 球棒击杀会等待攻击冷却满格，因此这里直接决定左键击杀节奏。 */
    public static final double PSYCHO_STAFF_ATTACK_SPEED = 1.3D;

    /** 疯魔法杖主手攻击速度属性修正值，最终显示/生效为 1.3 Attack Speed。 */
    public static final double PSYCHO_STAFF_ATTACK_SPEED_MODIFIER = PSYCHO_STAFF_ATTACK_SPEED - VANILLA_PLAYER_BASE_ATTACK_SPEED;

    /** HUD 把 tick 换算成秒时使用的每秒 tick 数。 */
    public static final int HUD_TICKS_PER_SECOND = 20;

    /** HUD 剩余秒数向上取整时补的 tick 数，避免 1.1 秒显示成 1 秒。 */
    public static final int HUD_SECOND_ROUNDING_TICKS = HUD_TICKS_PER_SECOND - 1;

    /** 客户端蓄力/冷却进度的完整显示值。 */
    public static final float HUD_PROGRESS_FULL = 1.0F;

    private LichConstants() {
    }
}
