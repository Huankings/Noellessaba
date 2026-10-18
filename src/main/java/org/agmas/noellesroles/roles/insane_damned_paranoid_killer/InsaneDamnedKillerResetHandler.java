package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 亡语杀手尸体伪装等玩家临时状态的硬重置规则。 */
public final class InsaneDamnedKillerResetHandler {
    private InsaneDamnedKillerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> InsaneDamnedKillerPlayerComponent.KEY.get(player).reset());
    }
}
