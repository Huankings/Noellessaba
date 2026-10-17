package org.agmas.noellesroles.roles.assassin;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 刺客物品与能力冷却的玩家硬重置规则。 */
public final class AssassinResetHandler {
    private AssassinResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(AssassinCooldownHelper::reset);
    }
}
