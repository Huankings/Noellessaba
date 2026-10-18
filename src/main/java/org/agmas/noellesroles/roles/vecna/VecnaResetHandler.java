package org.agmas.noellesroles.roles.vecna;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 维克那标记与颠倒状态的玩家硬重置规则。 */
public final class VecnaResetHandler {
    private VecnaResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> VecnaPlayerComponent.KEY.get(player).reset());
    }
}
