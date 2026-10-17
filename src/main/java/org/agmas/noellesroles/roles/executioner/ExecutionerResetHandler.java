package org.agmas.noellesroles.roles.executioner;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 刽子手目标与胜利进度的玩家硬重置规则。 */
public final class ExecutionerResetHandler {
    private ExecutionerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> ExecutionerPlayerComponent.KEY.get(player).reset());
    }
}
