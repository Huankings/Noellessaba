package org.agmas.noellesroles.roles.thief;

/** 小偷悦灵在服务端状态机和客户端 HUD 中共享的稳定状态。 */
public enum ThiefAllayState {
    /** 当前号数没有悦灵，也没有需要保留的终态提示。 */
    NONE,
    /** 悦灵正在寻找或飞向一个可偷窃的存活目标。 */
    SEEKING_TARGET,
    /** 悦灵已经偷到物品，正在返回小偷身边。 */
    RETURNING_SUCCESS,
    /** 悦灵没有偷到物品，正在空手返回小偷身边。 */
    RETURNING_FAILURE,
    /** 悦灵已经把物品抛给小偷，HUD 继续保留两秒。 */
    DELIVERY_SUCCESS,
    /** 悦灵已经死亡，HUD 继续保留两秒。 */
    DEAD;

    public boolean isLiving() {
        return this == SEEKING_TARGET || this == RETURNING_SUCCESS || this == RETURNING_FAILURE;
    }

    public boolean isTerminal() {
        return this == DELIVERY_SUCCESS || this == DEAD;
    }

    public static ThiefAllayState byOrdinal(int ordinal) {
        ThiefAllayState[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
    }
}
