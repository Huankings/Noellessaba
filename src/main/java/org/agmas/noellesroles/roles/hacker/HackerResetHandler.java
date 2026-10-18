package org.agmas.noellesroles.roles.hacker;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 黑客本体与手机连接状态的玩家硬重置规则。 */
public final class HackerResetHandler {
    private HackerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            HackerComponent.KEY.get(player).reset();
            HackerPhoneComponent.KEY.get(player).reset();
        });
    }
}
