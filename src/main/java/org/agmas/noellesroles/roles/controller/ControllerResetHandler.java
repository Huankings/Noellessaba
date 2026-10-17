package org.agmas.noellesroles.roles.controller;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 操控者与被操控者两侧组件的玩家硬重置规则。 */
public final class ControllerResetHandler {
    private ControllerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            ControllerPlayerComponent.KEY.get(player).reset();
            ControlledPlayerComponent.KEY.get(player).reset();
        });
    }
}
