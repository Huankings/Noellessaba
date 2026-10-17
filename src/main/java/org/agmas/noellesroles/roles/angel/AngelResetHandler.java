package org.agmas.noellesroles.roles.angel;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 天使守护/安抚状态的玩家硬重置规则。 */
public final class AngelResetHandler {
    private AngelResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> AngelPlayerComponent.KEY.get(player).reset());
    }
}
