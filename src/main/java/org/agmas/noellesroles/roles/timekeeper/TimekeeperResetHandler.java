package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.api.event.GameEvents;
import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 时停者玩家快照状态与世界回溯历史的回合清理。 */
public final class TimekeeperResetHandler {
    private TimekeeperResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> TimekeeperPlayerComponent.KEY.get(player).reset());
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                TimekeeperWorldComponent.KEY.get(serverWorld).reset();
            }
        });
    }
}
