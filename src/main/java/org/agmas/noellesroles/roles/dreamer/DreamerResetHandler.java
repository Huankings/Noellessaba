package org.agmas.noellesroles.roles.dreamer;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 梦想家及其杀手侧状态的玩家硬重置规则。 */
public final class DreamerResetHandler {
    private DreamerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            DreamerComponent.KEY.get(player).reset();
            DreamerKillerComponent.KEY.get(player).reset();
        });
    }
}
