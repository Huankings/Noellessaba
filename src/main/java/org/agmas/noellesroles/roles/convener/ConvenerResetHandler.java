package org.agmas.noellesroles.roles.convener;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 召集者玩家、伪装和动量三份运行态的硬重置规则。 */
public final class ConvenerResetHandler {
    private ConvenerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            ConvenerPlayerComponent.KEY.get(player).reset();
            ConvenerDisguiseComponent.KEY.get(player).clearDisguise();
            ConvenerMomentumComponent.KEY.get(player).reset();
        });
    }
}
