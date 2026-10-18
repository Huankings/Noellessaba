package org.agmas.noellesroles.roles.hunter;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 猎人目标与锁定状态的玩家硬重置规则。 */
public final class HunterResetHandler {
    private HunterResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> HunterPlayerComponent.KEY.get(player).reset());
    }
}
