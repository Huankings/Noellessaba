package org.agmas.noellesroles.roles.bartender;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 酒保本局状态的玩家硬重置规则。 */
public final class BartenderResetHandler {
    private BartenderResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> BartenderPlayerComponent.KEY.get(player).reset());
    }
}
