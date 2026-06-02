package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 灵术师职业的全部可调常量。
 *
 * <p>你要求后续方便直接调数值，因此这里把灵术师涉及到的关键参数全部集中：
 * 1. 附身判定距离；
 * 2. 出窍 / 附身结束后的冷却；
 * 3. 解除附身后的余留庇护时长；
 * 4. 本体位移后强制解除状态的距离阈值；
 * 5. 出窍时屏幕紫色叠层强度。
 */
public final class SpiritualistConstants {
    private SpiritualistConstants() {
    }

    /**
     * 灵术师职业颜色。
     */
    public static final int ROLE_COLOR = (81 << 16) | (54 << 8) | 217;

    /**
     * 对准玩家可发动附身的最大距离：2 格。
     */
    public static final float POSSESSION_RANGE = 2.0f;

    /**
     * 灵魂出窍结束后的固定冷却：45 秒。
     */
    public static final int PROJECTION_END_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);

    /**
     * 灵魂附身结束后的固定冷却：90 秒。
     */
    public static final int POSSESSION_END_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);

    /**
     * 正常解除附身后，目标身上残留庇护的持续时间：15 秒。
     */
    public static final int LINGERING_PROTECTION_TICKS = GameConstants.getInTicks(0, 15);

    /**
     * 当本体偏离记录位置超过这个平方距离时，判定为被传送 / 被异常移动，强制解除状态。
     *
     * <p>这里使用平方距离是为了避免每 tick 频繁开根号。</p>
     */
    public static final double BODY_MOVE_CANCEL_DISTANCE_SQUARED = 2.0d;

    /**
     * 灵体出窍最大活动半径。
     *
     * <p>这里沿用 spark 灵界行者的体验，避免灵体无限飞出对局区域。
     */
    public static final double PROJECTION_MAX_RADIUS = 30.0d;

    /**
     * 紫色灵视叠层的透明度。
     *
     * <p>当前实现优先保证稳定渲染，因此先采用全屏叠层而不是直接依赖后处理管线。
     * 后续如果你想改成真正的 post shader，也可以只替换客户端渲染实现，不需要动职业逻辑。</p>
     */
    public static final int PROJECTION_OVERLAY_ALPHA = 72;

    /**
     * 客户端灵魂相机使用的固定实体 ID。
     *
     * <p>取一个远离正常实体分配范围的负数，便于和真实世界实体区分。</p>
     */
    public static final int PROJECTION_CAMERA_ENTITY_ID = -421;

    /**
     * 客户端本地附身相机使用的固定实体 ID。
     *
     * <p>同样取远离正常实体分配范围的负数，避免和真实玩家或投影相机冲突。</p>
     */
    public static final int POSSESSION_CAMERA_ENTITY_ID = -422;

    /**
     * 客户端每 tick 给服务器发送一次附身控制数据时，使用的极小移动阈值。
     *
     * <p>当前先预留出来，便于后续需要进一步细化附身控制同步时统一调整。</p>
     */
    public static final float POSSESSION_CONTROL_EPSILON = 0.001f;
}
