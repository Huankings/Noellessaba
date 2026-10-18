package org.agmas.noellesroles.roles.starstruck;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 星界使者本局能力状态的玩家硬重置规则。 */
public final class StarstruckResetHandler {
    private StarstruckResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> StarstruckPlayerComponent.KEY.get(player).reset());
    }
}
