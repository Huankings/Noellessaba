package org.agmas.noellesroles.roles.thief;

/**
 * 小偷职业常量。
 *
 * <p>数值按用户确认从 StupidExpress 源码迁移，而不是按 guidebook 旧文案。</p>
 */
public final class ThiefConstants {
    public static final int ROLE_COLOR = 0x7a3002;
    public static final int STEAL_COOLDOWN_TICKS = 70 * 20;
    public static final int FAILED_STEAL_COOLDOWN_TICKS = STEAL_COOLDOWN_TICKS / 5;
    public static final double CLIENT_STEAL_RANGE = 1.0D;
    public static final double SERVER_STEAL_RANGE = 1.2D;
    public static final int TRACKER_INVENTORY_SCAN_INTERVAL_TICKS = 20;

    private ThiefConstants() {
    }
}
