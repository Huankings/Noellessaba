package org.agmas.noellesroles.roles.myers;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 迈尔斯阶段与能力状态的玩家硬重置规则。 */
public final class MyersResetHandler {
    private MyersResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> MyersPlayerComponent.KEY.get(player).reset());
    }
}
