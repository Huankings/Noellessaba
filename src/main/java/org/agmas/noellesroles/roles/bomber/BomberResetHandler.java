package org.agmas.noellesroles.roles.bomber;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 炸弹客放置/持有状态的玩家硬重置规则。 */
public final class BomberResetHandler {
    private BomberResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> BomberPlayerComponent.KEY.get(player).reset());
    }
}
