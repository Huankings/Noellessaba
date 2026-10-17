package org.agmas.noellesroles.roles.vulture;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 秃鹫食尸进度的玩家硬重置规则。 */
public final class VultureResetHandler {
    private VultureResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> VulturePlayerComponent.KEY.get(player).reset());
    }
}
