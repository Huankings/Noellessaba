package org.agmas.noellesroles.framing;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 共享伪装/栽赃系统的玩家临时状态清理。 */
public final class FramingResetHandler {
    private FramingResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> DelusionPlayerComponent.KEY.get(player).reset());
    }
}
