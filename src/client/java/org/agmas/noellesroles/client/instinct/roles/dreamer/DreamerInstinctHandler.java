package org.agmas.noellesroles.client.instinct.roles.dreamer;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.dreamer.DreamerComponent;

/**
 * 梦者本能与梦之印记标记高亮。
 */
public final class DreamerInstinctHandler {
    private DreamerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("dreamer_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.DREAMER)
                    && WatheClient.isInstinctInputActive()) {
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("dreamer_targets"), NoellesInstinctHandlers.PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (target instanceof PlayerEntity targetPlayer
                    && GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.DREAMER)
                    && WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.color(Noellesroles.DREAMER.color());
            }
            return InstinctApi.HighlightResult.pass();
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("dream_imprint"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            DreamerComponent targetDream = DreamerComponent.KEY.get(targetPlayer);
            if (targetDream.dreamerUuid == null || targetDream.dreamArmor <= 0) {
                return InstinctApi.HighlightResult.pass();
            }

            PlayerEntity dreamer = targetPlayer.getWorld().getPlayerByUuid(targetDream.dreamerUuid);
            boolean viewerIsDreamer = viewer == dreamer && WatheClient.isPlayerAliveAndInSurvival();
            if ((viewerIsDreamer && !WatheClient.isKiller())
                    || (viewerIsDreamer && WatheClient.isKiller() && !WatheClient.isInstinctEnabled())) {
                return InstinctApi.HighlightResult.color(Noellesroles.DREAMER.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
