package org.agmas.noellesroles.roles.operator;

/**
 * 接线员的全部数值常量统一收口到这里。
 *
 * <p>这样后续如果你想单独调整持续时间、成功冷却、失败冷却，
 * 只需要改这一处，不必再去能力类、组件类、聊天桥接类里逐个翻数字。</p>
 */
public final class OperatorConstants {

    /**
     * 接线持续时间：30 秒。
     */
    public static final int CONNECTION_DURATION_TICKS = 30 * 20;

    /**
     * 接线成功后的冷却：60 秒。
     */
    public static final int CONNECTION_SUCCESS_COOLDOWN_TICKS = 60 * 20;

    /**
     * 接线失败后的冷却：40 秒。
     */
    public static final int CONNECTION_FAILURE_COOLDOWN_TICKS = 40 * 20;

    /**
     * 广播持续时间：30 秒。
     */
    public static final int BROADCAST_DURATION_TICKS = 30 * 20;

    /**
     * 广播成功后的冷却：90 秒。
     */
    public static final int BROADCAST_SUCCESS_COOLDOWN_TICKS = 90 * 20;

    /**
     * 广播失败后的冷却：50 秒。
     */
    public static final int BROADCAST_FAILURE_COOLDOWN_TICKS = 50 * 20;

    private OperatorConstants() {
    }
}
