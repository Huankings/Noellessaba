package org.agmas.noellesroles.roles.magician;

import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 魔术师录制与播放状态的玩家硬重置规则。 */
public final class MagicianResetHandler {
    private MagicianResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            MagicianPlaybackManager.stopPlaybackSilently(player);
            MagicianPlayerComponent.KEY.get(player).reset();
        });
    }
}
