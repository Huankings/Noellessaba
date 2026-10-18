package org.agmas.noellesroles.roles.robot;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 机器人电量与能力状态的玩家硬重置规则。 */
public final class RobotResetHandler {
    private RobotResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> RobotPlayerComponent.KEY.get(player).reset());
    }
}
