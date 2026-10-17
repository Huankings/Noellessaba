package org.agmas.noellesroles.roles.spiritualist;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 灵术师本体、灵魂和宿主附身状态的玩家硬重置规则。 */
public final class SpiritualistResetHandler {
    private SpiritualistResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            SpiritualistPlayerComponent.KEY.get(player).reset();
            SpiritualistHostComponent.KEY.get(player).reset();
        });
    }
}
