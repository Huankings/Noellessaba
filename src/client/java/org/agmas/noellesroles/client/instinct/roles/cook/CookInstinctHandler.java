package org.agmas.noellesroles.client.instinct.roles.cook;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.cook.CookPlayerComponent;

import java.awt.Color;

/**
 * 厨师看到吃过东西的玩家。
 */
public final class CookInstinctHandler {
    private CookInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("cook_marks"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (target instanceof PlayerEntity targetPlayer
                    && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.COOK)
                    && WatheClient.isPlayerAliveAndInSurvival()
                    && CookPlayerComponent.KEY.get(targetPlayer).isMarkedByEating()) {
                return InstinctApi.HighlightResult.color(Color.GREEN.getRGB());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
