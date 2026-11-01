package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 小偷职业常量。
 *
 * <p>数值按用户确认从 StupidExpress 源码迁移，而不是按 guidebook 旧文案。</p>
 */
public final class ThiefConstants {
    /** 小偷职业颜色。 */
    public static final int ROLE_COLOR = 0x7a3002;

    /** 空手右键偷窃成功后的冷却秒数。 */
    public static final int STEAL_COOLDOWN_SECONDS = 70;

    /** 空手右键偷窃成功后的冷却 tick。 */
    public static final int STEAL_COOLDOWN_TICKS = GameConstants.getInTicks(1, 10);

    /** 空手右键偷窃失败后的冷却秒数。 */
    public static final int FAILED_STEAL_COOLDOWN_SECONDS = 14;

    /** 空手右键偷窃失败后的冷却 tick。 */
    public static final int FAILED_STEAL_COOLDOWN_TICKS = GameConstants.getInTicks(0, FAILED_STEAL_COOLDOWN_SECONDS);

    /** 客户端显示空手偷窃准心时使用的最大距离。 */
    public static final double CLIENT_STEAL_RANGE = 1.0D;

    /** 服务端权威校验空手偷窃时允许的最大距离。 */
    public static final double SERVER_STEAL_RANGE = 1.2D;

    /** 小偷胜利拖局物品扫描的间隔 tick。 */
    public static final int TRACKER_INVENTORY_SCAN_INTERVAL_TICKS = 20;

    /** 悦灵召唤能力的开局冷却秒数。 */
    public static final int ALLAY_INITIAL_COOLDOWN_SECONDS = 80;

    /** 悦灵召唤能力的开局冷却 tick。 */
    public static final int ALLAY_INITIAL_COOLDOWN_TICKS = GameConstants.getInTicks(1, 20);

    /** 召唤一只悦灵后的冷却秒数。 */
    public static final int ALLAY_ONE_COOLDOWN_SECONDS = 60;

    /** 召唤一只悦灵后的冷却 tick。 */
    public static final int ALLAY_ONE_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);

    /** 召唤两只悦灵后的冷却秒数。 */
    public static final int ALLAY_TWO_COOLDOWN_SECONDS = 120;

    /** 召唤两只悦灵后的冷却 tick。 */
    public static final int ALLAY_TWO_COOLDOWN_TICKS = GameConstants.getInTicks(2, 0);

    /** 召唤三只悦灵后的冷却秒数。 */
    public static final int ALLAY_THREE_COOLDOWN_SECONDS = 180;

    /** 召唤三只悦灵后的冷却 tick。 */
    public static final int ALLAY_THREE_COOLDOWN_TICKS = GameConstants.getInTicks(3, 0);

    /** 小偷每完成一个真实任务时减少的悦灵召唤冷却秒数。 */
    public static final int ALLAY_TASK_COOLDOWN_REDUCTION_SECONDS = 15;

    /** 小偷每完成一个真实任务时减少的悦灵召唤冷却 tick。 */
    public static final int ALLAY_TASK_COOLDOWN_REDUCTION_TICKS = GameConstants.getInTicks(0, 15);

    /** 一次最多召唤、同时也最多允许存活的悦灵数量。 */
    public static final int ALLAY_MAX_COUNT = 3;

    /** 一次召唤允许选择的最小悦灵数量。 */
    public static final int ALLAY_MIN_COUNT = 1;

    /** 数量选择界面中间按钮对应的悦灵数量。 */
    public static final int ALLAY_MIDDLE_COUNT = 2;

    /** Minecraft 每秒固定推进的 tick 数。 */
    public static final int TICKS_PER_SECOND = 20;

    /** 把剩余 tick 向上取整成秒时使用的偏移。 */
    public static final int COOLDOWN_SECONDS_ROUNDING_OFFSET = TICKS_PER_SECOND - 1;

    /** 悦灵靠近目标到该距离内时执行唯一一次偷窃判定。 */
    public static final double ALLAY_STEAL_DISTANCE = 1.5D;

    /** 悦灵靠近主人到该距离内时完成物品交付。 */
    public static final double ALLAY_DELIVERY_DISTANCE = 1.5D;

    /** 悦灵飞向目标或返回主人时使用的导航速度。 */
    public static final double ALLAY_NAVIGATION_SPEED = 1.65D;

    /** 悦灵重新向移动中的目标请求路径的间隔 tick。 */
    public static final int ALLAY_PATH_REFRESH_INTERVAL_TICKS = 10;

    /** 目标至少移动这些格后，悦灵才会提前放弃当前仍有效的完整路径并重新规划。 */
    public static final double ALLAY_DESTINATION_REPATH_DISTANCE = 3.0D;

    /** 目标提前重算距离的平方，逐 tick 判断时避免重复开平方。 */
    public static final double ALLAY_DESTINATION_REPATH_DISTANCE_SQUARED =
            ALLAY_DESTINATION_REPATH_DISTANCE * ALLAY_DESTINATION_REPATH_DISTANCE;

    /** 普通追踪连续没有拉近距离达到该时长后，开始搜索绕行路径。 */
    public static final int ALLAY_ROUTE_RECOVERY_TRIGGER_TICKS = GameConstants.getInTicks(0, 3);

    /**
     * 已获得完整绕路路径时允许暂时远离目标的最长秒数。
     * 超过后仍没有缩短最终距离，说明路径很可能在可走节点间徘徊，需要重新选路。
     */
    public static final int ALLAY_COMPLETE_ROUTE_RECOVERY_SECONDS = 12;

    /** 完整路线长期无最终距离进展后触发重新规划的 tick。 */
    public static final int ALLAY_COMPLETE_ROUTE_RECOVERY_TICKS =
            GameConstants.getInTicks(0, ALLAY_COMPLETE_ROUTE_RECOVERY_SECONDS);

    /** 当前路径连续返回不完整结果达到该次数后，允许提前进入绕行搜索。 */
    public static final int ALLAY_PARTIAL_PATH_FAILURE_LIMIT = 2;

    /** 绕行锚点最多跟随的秒数；超时后重新选择其它方向。 */
    public static final int ALLAY_RECOVERY_WAYPOINT_MAX_SECONDS = 8;

    /** 绕行锚点最多跟随的 tick。 */
    public static final int ALLAY_RECOVERY_WAYPOINT_MAX_TICKS =
            GameConstants.getInTicks(0, ALLAY_RECOVERY_WAYPOINT_MAX_SECONDS);

    /** 悦灵靠近绕行锚点到该距离后，认为本轮绕行完成并重新锁定最终目标。 */
    public static final double ALLAY_RECOVERY_WAYPOINT_REACHED_DISTANCE = 1.5D;

    /** 绕行锚点完成距离的平方。 */
    public static final double ALLAY_RECOVERY_WAYPOINT_REACHED_DISTANCE_SQUARED =
            ALLAY_RECOVERY_WAYPOINT_REACHED_DISTANCE * ALLAY_RECOVERY_WAYPOINT_REACHED_DISTANCE;

    /** 绕行期间连续几乎没有实际位移达到该时长后，立即更换绕行点。 */
    public static final int ALLAY_RECOVERY_MOVEMENT_STALL_TICKS = GameConstants.getInTicks(0, 2);

    /** 每次位置采样至少移动这些格，才认为悦灵没有在原地徘徊。 */
    public static final double ALLAY_MIN_ACTUAL_MOVEMENT_DISTANCE = 0.25D;

    /** 实际移动进展距离的平方。 */
    public static final double ALLAY_MIN_ACTUAL_MOVEMENT_DISTANCE_SQUARED =
            ALLAY_MIN_ACTUAL_MOVEMENT_DISTANCE * ALLAY_MIN_ACTUAL_MOVEMENT_DISTANCE;

    /** 在最终目标周围搜索可达落点时使用的水平半径。 */
    public static final int ALLAY_TARGET_APPROACH_HORIZONTAL_RADIUS = 4;

    /** 在最终目标周围搜索可达落点时向下检查的格数。 */
    public static final int ALLAY_TARGET_APPROACH_VERTICAL_BELOW = 2;

    /** 在最终目标周围搜索可达落点时向上检查的格数。 */
    public static final int ALLAY_TARGET_APPROACH_VERTICAL_ABOVE = 3;

    /** 随机绕行点相对悦灵的最大水平搜索距离。 */
    public static final int ALLAY_RECOVERY_HORIZONTAL_RANGE = 16;

    /** 随机绕行点相对悦灵的最大垂直搜索距离。 */
    public static final int ALLAY_RECOVERY_VERTICAL_RANGE = 8;

    /** 每轮绕行规划最多抽取的朝向目标候选数量。 */
    public static final int ALLAY_RECOVERY_TOWARD_ATTEMPTS = 12;

    /** 朝向目标的随机绕行搜索允许偏转的最大角度，单位为弧度。 */
    public static final double ALLAY_RECOVERY_MAX_ANGLE_RADIANS = Math.PI * 0.75D;

    /** 朝向目标的候选全部失败后，再抽取的无方向脱困候选数量。 */
    public static final int ALLAY_RECOVERY_ESCAPE_ATTEMPTS = 8;

    /** 绕行候选必须至少离当前悦灵这些格，避免再次选择原地附近的无效节点。 */
    public static final double ALLAY_RECOVERY_MIN_WAYPOINT_DISTANCE = 3.0D;

    /** 绕行候选最小距离的平方。 */
    public static final double ALLAY_RECOVERY_MIN_WAYPOINT_DISTANCE_SQUARED =
            ALLAY_RECOVERY_MIN_WAYPOINT_DISTANCE * ALLAY_RECOVERY_MIN_WAYPOINT_DISTANCE;

    /** 恢复路线搜索时传给导航器的精确到达容差。 */
    public static final int ALLAY_RECOVERY_PATH_ACCURACY = 0;

    /** 普通追踪实体时允许在目标一格内结束寻路。 */
    public static final int ALLAY_DIRECT_PATH_ACCURACY = 1;

    /**
     * 悦灵完整寻路时构建路径区块缓存的水平搜索范围。
     * 原版方块目标默认只取8格缓存，跨楼层时楼梯或门洞稍远就容易只得到贴墙的不完整路径。
     */
    public static final int ALLAY_EXTENDED_PATH_SEARCH_RANGE = 32;

    /** 悦灵判断是否取得有效距离进展的采样间隔 tick。 */
    public static final int ALLAY_PROGRESS_CHECK_INTERVAL_TICKS = 20;

    /** 每次进展采样至少需要缩短的距离，低于该值不视为有效进展。 */
    public static final double ALLAY_MIN_PROGRESS_DISTANCE = 0.35D;

    /** 悦灵连续无目标或无有效进展后自动死亡的秒数。 */
    public static final int ALLAY_NO_PROGRESS_TIMEOUT_SECONDS = 120;

    /** 悦灵连续无目标或无有效进展后自动死亡的 tick。 */
    public static final int ALLAY_NO_PROGRESS_TIMEOUT_TICKS = GameConstants.getInTicks(2, 0);

    /** “交付成功”和“不幸身亡”在 HUD 中保留的秒数。 */
    public static final int ALLAY_TERMINAL_HUD_SECONDS = 2;

    /** “交付成功”和“不幸身亡”在 HUD 中保留的 tick。 */
    public static final int ALLAY_TERMINAL_HUD_TICKS = GameConstants.getInTicks(0, 2);

    /** 悦灵实体的碰撞宽度。 */
    public static final float ALLAY_ENTITY_WIDTH = 0.35F;

    /** 悦灵实体的碰撞高度。 */
    public static final float ALLAY_ENTITY_HEIGHT = 0.6F;

    /** 悦灵实体向客户端同步的最大追踪距离，单位为区块。 */
    public static final int ALLAY_TRACKING_RANGE = 10;

    /** 悦灵实体位置与数据的同步间隔 tick。 */
    public static final int ALLAY_TRACKING_INTERVAL_TICKS = 1;

    /** Wathe 匕首与刺刀命中悦灵时使用的最大距离。 */
    public static final float ALLAY_KNIFE_HIT_RANGE = 3.0F;

    /** Wathe 手雷和无声手雷击杀悦灵的爆炸半径。 */
    public static final float ALLAY_GRENADE_RADIUS = 3.0F;

    /** 匕首包兼容 mixin 的优先级，需高于普通职业记录层。 */
    public static final int ALLAY_KNIFE_MIXIN_PRIORITY = 1_200;

    /** 客户端匕首目标 mixin 优先级，低于魔术师播放体目标以保留可见遮挡顺序。 */
    public static final int ALLAY_KNIFE_TARGET_MIXIN_PRIORITY = 900;

    /** 悦灵被击杀时播放原版死亡音效的音量。 */
    public static final float ALLAY_DEATH_SOUND_VOLUME = 1.0F;

    /** 悦灵被击杀时播放原版死亡音效的音调。 */
    public static final float ALLAY_DEATH_SOUND_PITCH = 1.0F;

    /** 冷却组件每秒向客户端校准一次时使用的 tick 间隔。 */
    public static final int ALLAY_COOLDOWN_SYNC_INTERVAL_TICKS = 20;

    /** 比较枪械默认目标与悦灵命中距离时使用的浮点容差。 */
    public static final double ALLAY_TARGET_DISTANCE_EPSILON = 1.0E-4D;

    /** Wathe 普通枪击包对左轮类武器沿用的服务端最大距离。 */
    public static final float ALLAY_DEFAULT_GUN_SERVER_RANGE = 65.0F;

    /** 判断实体几乎没有移动时使用的平方速度容差。 */
    public static final double ALLAY_MOVEMENT_EPSILON_SQUARED = 1.0E-8D;

    /** 扫描门碰撞时向悦灵扫掠包围盒附加的容差。 */
    public static final double ALLAY_DOOR_COLLISION_EXPANSION = 0.02D;

    /** 多只悦灵生成时围绕小偷分散的水平半径。 */
    public static final double ALLAY_SPAWN_RADIUS = 0.8D;

    /** 悦灵生成位置相对小偷脚底的垂直偏移。 */
    public static final double ALLAY_SPAWN_HEIGHT = 1.0D;

    /** 悦灵交付物品时朝小偷方向施加的水平速度。 */
    public static final double ALLAY_DELIVERY_ITEM_SPEED = 0.22D;

    /** 悦灵交付物品时附加的向上速度。 */
    public static final double ALLAY_DELIVERY_ITEM_UPWARD_SPEED = 0.12D;

    /** 悦灵交付掉落物的命令标签前缀，用于精确放行小偷拾取左轮。 */
    public static final String ALLAY_DELIVERY_TAG_PREFIX = "noellesroles_thief_allay_delivery_";

    /** 悦灵枪击接管优先级，高于各职业枪械的普通处理器。 */
    public static final int ALLAY_GUN_HANDLER_PRIORITY = 2_000;

    /** 悦灵枪械目标覆写优先级。 */
    public static final int ALLAY_GUN_TARGET_PRIORITY = 2_000;

    /** 悦灵交付左轮拾取特权优先级，低于重伤等强制禁拾规则。 */
    public static final int ALLAY_REVOLVER_PICKUP_PRIORITY = 100;

    /** 悦灵 HUD 的屏幕右边距。 */
    public static final int HUD_RIGHT_MARGIN = 10;

    /** 悦灵 HUD 的屏幕下边距。 */
    public static final int HUD_BOTTOM_MARGIN = 10;

    /** 背包头像和数量按钮的宽高。 */
    public static final int INVENTORY_BUTTON_SIZE = 16;

    /** 背包按钮外框的绘制宽高。 */
    public static final int INVENTORY_BUTTON_FRAME_SIZE = 30;

    /** 背包按钮外框相对物品图标的左上偏移。 */
    public static final int INVENTORY_BUTTON_FRAME_OFFSET = 7;

    /** 背包二级界面标题相对按钮行向上的额外偏移。 */
    public static final int INVENTORY_TITLE_OFFSET = 8;

    /** 冷却中按钮使用的灰度 RGB 分量。 */
    public static final float INVENTORY_BLOCKED_SHADER_RGB = 0.25F;

    /** 冷却中按钮使用的透明度。 */
    public static final float INVENTORY_BLOCKED_SHADER_ALPHA = 0.5F;

    /** 按钮悬停高亮沿用项目其它背包按钮的 ARGB 颜色。 */
    public static final int INVENTORY_HOVER_COLOR = -1862287543;

    /** 深红色失败 ActionBar 使用的 RGB 颜色。 */
    public static final int FAILURE_ACTIONBAR_COLOR = 0xAA0000;

    /** 小偷本能透视悦灵时使用的灰色 ARGB，与 {@code Color.GRAY.getRGB()} 完全一致。 */
    public static final int ALLAY_INSTINCT_COLOR = 0xFF808080;

    public static int getAllayCooldownTicks(int count) {
        return switch (count) {
            case 1 -> ALLAY_ONE_COOLDOWN_TICKS;
            case 2 -> ALLAY_TWO_COOLDOWN_TICKS;
            case 3 -> ALLAY_THREE_COOLDOWN_TICKS;
            default -> 0;
        };
    }

    private ThiefConstants() {
    }
}
