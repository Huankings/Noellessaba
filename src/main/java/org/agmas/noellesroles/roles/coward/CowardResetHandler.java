package org.agmas.noellesroles.roles.coward;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 懦夫与镇静剂临时状态的玩家硬重置规则。 */
public final class CowardResetHandler {
    private CowardResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            CowardPlayerComponent.KEY.get(player).reset();
            SedativePlayerComponent.KEY.get(player).reset();
        });
    }
}
