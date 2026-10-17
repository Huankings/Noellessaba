package org.agmas.noellesroles.roles.recaller;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 召回者位置记录状态的玩家硬重置规则。 */
public final class RecallerResetHandler {
    private RecallerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> RecallerPlayerComponent.KEY.get(player).reset());
    }
}
