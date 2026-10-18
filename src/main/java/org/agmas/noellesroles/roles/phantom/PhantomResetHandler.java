package org.agmas.noellesroles.roles.phantom;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 幻影本局状态的玩家硬重置规则。 */
public final class PhantomResetHandler {
    private PhantomResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> PhantomPlayerComponent.KEY.get(player).reset());
    }
}
