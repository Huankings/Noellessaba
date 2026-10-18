package org.agmas.noellesroles.roles.kidnapper;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 绑匪与被控制者的完整玩家硬重置规则。 */
public final class KidnapperResetHandler {
    private KidnapperResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> KidnapperComponent.KEY.get(player).resetAll());
    }
}
