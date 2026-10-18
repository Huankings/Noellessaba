package org.agmas.noellesroles.roles.perceiver;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 感知者观察状态的玩家硬重置规则。 */
public final class PerceiverResetHandler {
    private PerceiverResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> PerceiverPlayerComponent.KEY.get(player).reset());
    }
}
