package org.agmas.noellesroles.roles.arsonist;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 纵火犯在其他玩家身上留下的浇油状态清理。 */
public final class ArsonistResetHandler {
    private ArsonistResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> DousedPlayerComponent.KEY.get(player).reset());
    }
}
