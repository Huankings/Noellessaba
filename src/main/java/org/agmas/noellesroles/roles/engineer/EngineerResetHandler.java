package org.agmas.noellesroles.roles.engineer;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.minecraft.util.TypeFilter;
import org.agmas.noellesroles.entities.CaptureDeviceEntity;

/** 工程师捕捉装置实体的回合结束清理。玩家定身组件会由自身状态流程解除。 */
public final class EngineerResetHandler {
    private EngineerResetHandler() {}
    public static void init() {
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                for (CaptureDeviceEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(CaptureDeviceEntity.class), ignored -> true)) {
                    entity.discard();
                }
            }
        });
    }
}
