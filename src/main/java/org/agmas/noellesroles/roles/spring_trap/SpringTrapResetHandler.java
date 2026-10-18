package org.agmas.noellesroles.roles.spring_trap;

import dev.doctor4t.wathe.api.event.GameEvents;

/** 弹簧陷阱世界范围光环状态的回合结束清理。 */
public final class SpringTrapResetHandler {
    private SpringTrapResetHandler() {}
    public static void init() {
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                SpringTrapAuraWorldComponent.KEY.get(serverWorld).reset();
            }
        });
    }
}
