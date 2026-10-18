package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.event.GameEvents;
import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 杀手皇后玩家标记状态与世界级招募/标记表清理。 */
public final class KillerQueenResetHandler {
    private KillerQueenResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> KillerQueenPlayerComponent.KEY.get(player).reset());
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                KillerQueenWorldComponent.KEY.get(serverWorld).clear();
            }
        });
    }
}
