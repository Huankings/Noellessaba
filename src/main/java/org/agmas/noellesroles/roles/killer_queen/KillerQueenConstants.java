package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.game.GameConstants;

import java.awt.Color;

/** 杀手皇后的全部可调玩法数值。 */
public final class KillerQueenConstants {
    /** 杀手皇后职业色：RGB(80, 180, 250)。 */
    public static final int ROLE_COLOR = new Color(80, 180, 250).getRGB();
    /** 主动本能观察到已被当前杀手皇后标记的玩家时使用的区分色：RGB(100, 40, 200)。 */
    public static final int MARKED_INSTINCT_COLOR = new Color(100, 40, 200).getRGB();
    /** Harpy 随机分配时一局最多生成一名杀手皇后。 */
    public static final int MAX_ROLE_COUNT = 1;
    /** 无限体力职业传给 Wathe Role 的最大冲刺时间标记。 */
    public static final int INFINITE_SPRINT_TIME_TICKS = -1;
    /** 三件杀手皇后专属物品的最大堆叠数。 */
    public static final int ITEM_MAX_COUNT = 1;
    /** 商店一次购买便条时交付的数量。 */
    public static final int NOTE_STACK_COUNT = 4;
    /** 公共能力准心判定距离，单位为方块。 */
    public static final float ABILITY_RANGE_BLOCKS = 2.5F;
    /** 服务端能力距离平方，避免每次判定开平方。 */
    public static final double ABILITY_RANGE_SQUARED = ABILITY_RANGE_BLOCKS * ABILITY_RANGE_BLOCKS;
    /** 开局公共能力冷却。 */
    public static final int INITIAL_ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    /** 洗牌杀手职业消耗的金币。 */
    public static final int SHUFFLE_COST_COINS = 100;
    /** 成功洗牌后的公共能力冷却。 */
    public static final int SHUFFLE_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    /** 招募普通目标消耗的金币；模仿者不支付该费用。 */
    public static final int RECRUIT_COST_COINS = 150;
    /** 成功招募后的公共能力冷却。 */
    public static final int RECRUIT_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    /** 被招募者完成目标并转成杀手后的初始金币。 */
    public static final int RECRUITED_KILLER_STARTING_COINS = 200;
    /** 杀手皇后每完成一个任务获得的金币。 */
    public static final int TASK_INCOME_COINS = 50;
    /** 任意杀手阵营玩家确认击杀后，存活杀手皇后获得的金币。 */
    public static final int KILLER_KILL_REWARD_COINS = 25;
    /** 酸液商店价格。 */
    public static final int ACID_SOLUTION_PRICE_COINS = 50;
    /** 酸液命中好人或独立中立消费者时给予施加者的金币。 */
    public static final int ACID_CONSUME_REWARD_COINS = 50;
    /** 好人或独立中立玩家亲自食用/饮用酸液时获得的金币奖励。 */
    public static final int ACID_CONSUMER_REWARD_COINS = 50;
    /** 杀手皇后或机器人食用酸液后获得的速度倍率。 */
    public static final double ACID_SPEED_MULTIPLIER = 2.1D;
    /** 酸液速度加成持续时间。 */
    public static final int ACID_SPEED_DURATION_TICKS = GameConstants.getInTicks(0, 15);
    /** 其他玩家食用酸液后反胃和失明的持续时间。 */
    public static final int ACID_DAZE_DURATION_TICKS = GameConstants.getInTicks(0, 10);
    /** 酸液反胃效果等级；0 表示药水一级。 */
    public static final int ACID_NAUSEA_AMPLIFIER = 0;
    /** 酸液失明效果等级；0 表示药水一级。 */
    public static final int ACID_BLINDNESS_AMPLIFIER = 0;
    /** 标记法杖完整攻击速度。 */
    public static final double MARKER_STAFF_ATTACK_SPEED = 1.0D;
    /** 原版玩家基础攻击速度，用于换算物品属性修正。 */
    public static final double VANILLA_PLAYER_BASE_ATTACK_SPEED = 4.0D;
    /** 标记法杖主手攻击速度修正值。 */
    public static final double MARKER_STAFF_ATTACK_SPEED_MODIFIER = MARKER_STAFF_ATTACK_SPEED - VANILLA_PLAYER_BASE_ATTACK_SPEED;
    /** 标记法杖命中后的定身时间。 */
    public static final int MARKER_STAFF_ROOT_TICKS = GameConstants.getInTicks(0, 5);
    /** 标记法杖首次标记合法目标时增加的疯魔时间。 */
    public static final int MARKER_STAFF_PSYCHO_BONUS_TICKS = GameConstants.getInTicks(0, 10);
    /** 标记法杖每首次标记多少个合法目标，就奖励一次疯魔护盾。 */
    public static final int MARKER_STAFF_VALID_MARKS_PER_SHIELD = 2;
    /** 达到标记次数门槛后增加的疯魔护盾层数。 */
    public static final int MARKER_STAFF_SHIELD_REWARD_LAYERS = 1;
    /** 杀手皇后疯魔初始持续时间。 */
    public static final int PSYCHO_DURATION_TICKS = GameConstants.getInTicks(0, 52);
    /** 杀手皇后疯魔初始护盾层数。 */
    public static final int PSYCHO_SHIELD_LAYERS = 1;
    /** 杀手皇后疯魔商店价格。 */
    public static final int PSYCHO_PRICE_COINS = 350;
    /** 杀手皇后疯魔购买后的物品冷却。 */
    public static final int PSYCHO_COOLDOWN_TICKS = GameConstants.getInTicks(4, 30);
    /** 被杀手皇后疯魔强制终止的其它疯魔，其对应图标最低冷却。 */
    public static final int SUPPRESSED_PSYCHO_MIN_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    /** 杀手皇后疯魔期间召集者能力被持续托底的冷却。 */
    public static final int CONVENER_MIN_ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 4);
    /** 幻觉试剂在杀手皇后商店中的价格。 */
    public static final int DELUSION_VIAL_PRICE_COINS = 20;
    /** 开锁器在杀手皇后商店中的价格。 */
    public static final int LOCKPICK_PRICE_COINS = 100;
    /** 鞭炮在杀手皇后商店中的价格。 */
    public static final int FIRECRACKER_PRICE_COINS = 10;
    /** 便条在杀手皇后商店中的价格。 */
    public static final int NOTE_PRICE_COINS = 10;
    /** 酸液托盘粒子每次客户端 tick 实际生成的概率分母。 */
    public static final int TRAY_PARTICLE_CHANCE_DENOMINATOR = 4;
    /** 酸液托盘粒子大小。 */
    public static final float TRAY_PARTICLE_SCALE = 1.0F;
    /** 酸液托盘淡蓝色粒子的红色分量。 */
    public static final float TRAY_BLUE_RED = 0.35F;
    /** 酸液托盘淡蓝色粒子的绿色分量。 */
    public static final float TRAY_BLUE_GREEN = 0.8F;
    /** 酸液托盘淡蓝色粒子的蓝色分量。 */
    public static final float TRAY_BLUE_BLUE = 1.0F;
    /** 酸液托盘绿色粒子的红色分量。 */
    public static final float TRAY_GREEN_RED = 0.2F;
    /** 酸液托盘绿色粒子的绿色分量。 */
    public static final float TRAY_GREEN_GREEN = 1.0F;
    /** 酸液托盘绿色粒子的蓝色分量。 */
    public static final float TRAY_GREEN_BLUE = 0.25F;
    /** 酸液托盘粒子生成高度偏移。 */
    public static final double TRAY_PARTICLE_Y_OFFSET = 0.25D;
    /** 酸液托盘粒子相对方块中心的水平偏移。 */
    public static final double TRAY_PARTICLE_HORIZONTAL_OFFSET = 0.5D;
    /** 酸液托盘粒子向上漂浮速度。 */
    public static final double TRAY_PARTICLE_UP_SPEED = 0.04D;
    /** 疯魔 HUD provider 比 Wathe 默认样式更高的优先级增量。 */
    public static final int PSYCHO_MOOD_HUD_PRIORITY_BONUS = 200;
    /** 杀手皇后本能启用规则优先级。 */
    public static final int INSTINCT_AVAILABILITY_PRIORITY = 200;
    /** 杀手皇后疯魔反透视规则优先级。 */
    public static final int PSYCHO_INSTINCT_HIDE_PRIORITY = 5000;
    /** 杀手皇后主动本能颜色规则优先级。 */
    public static final int ACTIVE_INSTINCT_COLOR_PRIORITY = 350;
    /** 杀手皇后标记被动透视优先级，必须低于主动本能。 */
    public static final int MARK_PASSIVE_HIGHLIGHT_PRIORITY = 40;
    /** 被招募者目标被动透视优先级。 */
    public static final int RECRUIT_TARGET_HIGHLIGHT_PRIORITY = 60;
    /** 标记法杖死亡早期拦截相对 Wathe 特殊存活保护的优先级增量。 */
    public static final int MARKER_STAFF_DEATH_PRIORITY_BONUS = 700;
    /** 疯魔期间杀手侧致死压制的优先级增量。 */
    public static final int PSYCHO_KILL_SUPPRESSION_PRIORITY_BONUS = 600;
    /** 被招募者目标限制的优先级增量。 */
    public static final int RECRUIT_TARGET_ONLY_PRIORITY_BONUS = 500;
    /** 招募目标死亡前解除生命绑定的优先级增量。 */
    public static final int RECRUIT_UNBIND_PRIORITY_BONUS = 1000;
    /** 被招募者左轮惩罚豁免规则优先级。 */
    public static final int RECRUIT_REVOLVER_PENALTY_PRIORITY = 500;
    /** 杀手皇后专属胜利规则高于 Wathe 默认仲裁的优先级增量。 */
    public static final int VICTORY_PRIORITY_BONUS = 50;
    /** 杀手皇后疯魔音乐播放音量。 */
    public static final float PSYCHO_MUSIC_VOLUME = 0.6F;
    /** 杀手皇后疯魔音乐播放音高。 */
    public static final float PSYCHO_MUSIC_PITCH = 1.0F;
    /** HUD 秒数向上取整使用的一秒 tick 数。 */
    public static final int TICKS_PER_SECOND = 20;

    private KillerQueenConstants() {
    }
}
