package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.world.ServerWorld;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.NoellesRolesEntities;

/** 小偷玩家组件和悦灵实体的回合硬清理。 */
public final class ThiefResetHandler {
    private static boolean initialized;

    private ThiefResetHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ResetPlayerEvent.EVENT.register(player -> ThiefPlayerComponent.KEY.get(player).reset());

        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof ServerWorld serverWorld) {
                for (ThiefAllayEntity allay : serverWorld.getEntitiesByType(
                        NoellesRolesEntities.THIEF_ALLAY_ENTITY_TYPE,
                        entity -> true
                )) {
                    /* 对局结束不掉落、不显示死亡 HUD、不写回放，避免结算后继续制造状态。 */
                    allay.discardSilently();
                }
            }
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ThiefAllayEntity allay
                    && !dev.doctor4t.wathe.cca.GameWorldComponent.KEY.get(world).isRunning()) {
                /* 处理未加载区块中残留到下一局才重新加载的旧悦灵。 */
                allay.discardSilently();
            }
        });
    }
}
