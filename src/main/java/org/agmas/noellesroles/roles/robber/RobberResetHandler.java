package org.agmas.noellesroles.roles.robber;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.minecraft.util.TypeFilter;
import org.agmas.harpymodloader.events.ResetPlayerEvent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.entities.ThrowingAxeEntity;

/** 强盗投斧和手枪实际物品冷却的玩家硬重置规则。 */
public final class RobberResetHandler {
    private RobberResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            player.getItemCooldownManager().remove(ModItems.THROWING_AXE);
            player.getItemCooldownManager().remove(ModItems.ROBBER_PISTOL);
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                for (ThrowingAxeEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(ThrowingAxeEntity.class), ignored -> true)) {
                    entity.discard();
                }
            }
        });
    }
}
