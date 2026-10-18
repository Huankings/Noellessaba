package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.event.GameEvents;
import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 法外狂徒玩家状态与世界级团伙状态清理。 */
public final class OutlawResetHandler {
    private OutlawResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> OutlawPlayerComponent.KEY.get(player).reset());
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                OutlawWorldComponent.KEY.get(serverWorld).reset();
            }
        });
    }
}
