package org.agmas.noellesroles.roles.muzzler;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 静语者施加的沉默状态玩家硬重置规则。 */
public final class MuzzlerResetHandler {
    private MuzzlerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> SilencePlayerComponent.KEY.get(player).reset());
    }
}
