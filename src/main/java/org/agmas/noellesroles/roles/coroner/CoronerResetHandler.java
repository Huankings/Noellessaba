package org.agmas.noellesroles.roles.coroner;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 验尸官调查状态的玩家硬重置规则。 */
public final class CoronerResetHandler {
    private CoronerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> CoronerPlayerComponent.KEY.get(player).reset());
    }
}
