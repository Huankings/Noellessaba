package org.agmas.noellesroles.roles.morphling;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 变形者自身变形状态的玩家硬重置规则。 */
public final class MorphlingResetHandler {
    private MorphlingResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> MorphlingPlayerComponent.KEY.get(player).reset());
    }
}
