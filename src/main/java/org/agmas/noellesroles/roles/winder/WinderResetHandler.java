package org.agmas.noellesroles.roles.winder;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 风灵师选择、漂浮和风印状态的玩家硬重置规则。 */
public final class WinderResetHandler {
    private WinderResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            WinderPlayerComponent.KEY.get(player).reset();
            WindMarkPlayerComponent.KEY.get(player).reset();
        });
    }
}
