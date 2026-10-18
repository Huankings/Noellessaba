package org.agmas.noellesroles.roles.magician;

import dev.doctor4t.wathe.api.event.GameEvents;
import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 魔术师录制与播放状态的玩家硬重置规则。 */
public final class MagicianResetHandler {
    private MagicianResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            MagicianPlaybackManager.stopPlaybackSilently(player);
            MagicianPlayerComponent.KEY.get(player).reset();
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                MagicianPlaybackManager.cleanupAllPlaybackEntities(serverWorld);
            }
        });
    }
}
