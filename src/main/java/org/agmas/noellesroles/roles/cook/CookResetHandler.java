package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.minecraft.util.TypeFilter;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.entities.ThrowingPanEntity;

/** 厨师玩家状态与仍在世界中的投掷锅清理。 */
public final class CookResetHandler {
    private CookResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> CookPlayerComponent.KEY.get(player).reset());
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                for (ThrowingPanEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(ThrowingPanEntity.class), ignored -> true)) {
                    entity.discard();
                }
            }
        });
    }
}
