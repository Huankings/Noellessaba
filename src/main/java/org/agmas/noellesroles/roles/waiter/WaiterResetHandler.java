package org.agmas.noellesroles.roles.waiter;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 服务员托盘与任务运行态的玩家硬重置规则。 */
public final class WaiterResetHandler {
    private WaiterResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> WaiterPlayerComponent.KEY.get(player).reset());
    }
}
