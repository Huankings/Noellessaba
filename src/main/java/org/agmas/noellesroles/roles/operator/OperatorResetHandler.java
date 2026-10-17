package org.agmas.noellesroles.roles.operator;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 接线员连接与目标状态的玩家硬重置规则。 */
public final class OperatorResetHandler {
    private OperatorResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> OperatorPlayerComponent.KEY.get(player).reset());
    }
}
