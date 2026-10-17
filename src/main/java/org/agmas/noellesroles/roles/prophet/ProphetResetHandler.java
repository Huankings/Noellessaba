package org.agmas.noellesroles.roles.prophet;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 先知水晶球与巫毒免伤状态的玩家硬重置规则。 */
public final class ProphetResetHandler {
    private ProphetResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> ProphetPlayerComponent.KEY.get(player).reset());
    }
}
