package org.agmas.noellesroles.roles.voodoo;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 巫毒师绑定状态的玩家硬重置规则。 */
public final class VoodooResetHandler {
    private VoodooResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> VoodooPlayerComponent.KEY.get(player).reset());
    }
}
