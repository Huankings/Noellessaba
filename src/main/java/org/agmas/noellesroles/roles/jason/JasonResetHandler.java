package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.event.GameEvents;
import net.minecraft.util.TypeFilter;
import org.agmas.harpymodloader.events.ResetPlayerEvent;

/** 杰森伤势、能力、失明、火焰和投掷武器的完整回合清理。 */
public final class JasonResetHandler {
    private JasonResetHandler() {}
    public static void init() {
        ResetPlayerEvent.EVENT.register(player -> {
            JasonWoundManager.resetPlayer(player);
            JasonAbilityManager.resetPlayer(player);
            JasonAbilityBlindnessComponent.KEY.get(player).reset();
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, game) -> {
            if (!(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
                return;
            }
            JasonFireWorldComponent.KEY.get(serverWorld).reset();
            JasonWoundManager.resetRoundTransientState();
            JasonAbilityManager.resetRoundTransientState(serverWorld);
            for (JasonThrownWeaponEntity entity : serverWorld.getEntitiesByType(TypeFilter.equals(JasonThrownWeaponEntity.class), ignored -> true)) {
                entity.discard();
            }
        });
    }
}
