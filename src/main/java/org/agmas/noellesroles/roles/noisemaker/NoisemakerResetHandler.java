package org.agmas.noellesroles.roles.noisemaker;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 噪音制造者本局触发状态的玩家硬重置规则。 */
public final class NoisemakerResetHandler {
    private NoisemakerResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> NoisemakerPlayerComponent.KEY.get(player).reset());
    }
}
