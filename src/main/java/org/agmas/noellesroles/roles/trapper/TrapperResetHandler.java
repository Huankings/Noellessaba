package org.agmas.noellesroles.roles.trapper;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.minecraft.util.TypeFilter;
import org.agmas.noellesroles.entities.RoleMineEntity;

/** 陷阱师仍留在世界中的职业地雷回合结束清理。 */
public final class TrapperResetHandler {
    private TrapperResetHandler() {}
    public static void init() {
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                for (RoleMineEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(RoleMineEntity.class), ignored -> true)) {
                    entity.discard();
                }
            }
        });
    }
}
