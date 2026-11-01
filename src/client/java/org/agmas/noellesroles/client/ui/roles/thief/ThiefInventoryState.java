package org.agmas.noellesroles.client.ui.roles.thief;

import java.util.UUID;

/** 仅在当前打开的背包界面中使用的客户端临时选择状态。 */
public final class ThiefInventoryState {
    public static ThiefInventoryPhase phase = ThiefInventoryPhase.PLAYER_SELECT;
    public static UUID selectedTarget;

    private ThiefInventoryState() {
    }

    public static void reset() {
        phase = ThiefInventoryPhase.PLAYER_SELECT;
        selectedTarget = null;
    }
}
