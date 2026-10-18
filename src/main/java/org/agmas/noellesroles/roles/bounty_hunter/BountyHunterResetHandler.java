package org.agmas.noellesroles.roles.bounty_hunter;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 赏金猎人目标与奖励状态的玩家硬重置规则。 */
public final class BountyHunterResetHandler {
    private BountyHunterResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> BountyHunterPlayerComponent.KEY.get(player).reset());
    }
}
