package org.agmas.noellesroles.client.instinct.roles.winder;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.winder.WindMarkPlayerComponent;

public final class WinderInstinctHandler {
    private WinderInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("winder_mark"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            WindMarkPlayerComponent windMark = WindMarkPlayerComponent.KEY.get(targetPlayer);
            if (gameWorld.isRole(viewer, NoellesRoleRegistry.WINDER)
                    && WatheClient.isPlayerAliveAndInSurvival()
                    && windMark.hasActiveMark()
                    && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    && Wathe.isSkyVisibleAdjacent(target)) {
                /*
                 * Winder 风印是职业能力留下的目标标记，不是本能键本身。
                 * 因此这里不检查 WatheClient.isInstinctEnabled()，避免 Convener 只压制本能时误关能力提示。
                 */
                return InstinctApi.HighlightResult.color(NoellesRoleRegistry.WINDER.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
