package org.agmas.noellesroles.roles.stalker;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 潜行者目标和处决进度的玩家硬重置规则。 */
public final class StalkerResetHandler {
    private StalkerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> StalkerPlayerComponent.KEY.get(player).reset());
    }
}
