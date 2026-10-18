package org.agmas.noellesroles.roles.physician;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 医师诊断与治疗状态的玩家硬重置规则。 */
public final class PhysicianResetHandler {
    private PhysicianResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> PhysicianPlayerComponent.KEY.get(player).reset());
    }
}
