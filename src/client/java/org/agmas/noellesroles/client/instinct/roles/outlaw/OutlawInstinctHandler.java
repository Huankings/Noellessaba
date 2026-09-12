package org.agmas.noellesroles.client.instinct.roles.outlaw;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.outlaw.OutlawConstants;

/** 亡命徒对其它存活玩家的灰色本能透视。 */
public final class OutlawInstinctHandler {
    private OutlawInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("outlaw_availability"), InstinctApi.DEFAULT_PRIORITY, viewer ->
                GameFunctions.isPlayerAliveAndSurvival(viewer)
                        && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.OUTLAW)
                        && WatheClient.isInstinctInputActive()
                        ? InstinctApi.AvailabilityResult.ENABLE
                        : InstinctApi.AvailabilityResult.PASS
        );
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("outlaw_targets"), NoellesInstinctHandlers.PRIORITY_ROLE_INSTINCT_COLOR, (viewer, target) ->
                target instanceof PlayerEntity targetPlayer
                        && targetPlayer != viewer
                        && GameFunctions.isPlayerAliveAndSurvival(viewer)
                        && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                        && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.OUTLAW)
                        && WatheClient.isInstinctEnabled()
                        ? InstinctApi.HighlightResult.color(OutlawConstants.INSTINCT_COLOR)
                        : InstinctApi.HighlightResult.pass()
        );
    }
}
