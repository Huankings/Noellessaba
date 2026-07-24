package org.agmas.noellesroles.client.instinct.roles.hacker;

import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.hacker.HackerComponent;
import org.agmas.noellesroles.roles.hacker.HackerConstants;

import java.awt.Color;

/**
 * 黑客本能高亮。
 */
public final class HackerInstinctHandler {
    private HackerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("hacker_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.HACKER)
                    && WatheClient.isInstinctInputActive()) {
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("hacker_targets"), NoellesInstinctHandlers.PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.HACKER)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            Role targetRole = gameWorld.getRole(targetPlayer);
            if (targetRole == null) {
                return InstinctApi.HighlightResult.pass();
            }

            if (gameWorld.canUseKillerFeatures(targetPlayer) || gameWorld.isRole(targetPlayer, NoellesRoleRegistry.MIMIC)) {
                return InstinctApi.HighlightResult.color(MathHelper.hsvToRgb(0.0F, 1.0F, 0.6F));
            }
            if (NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(targetRole)) {
                return InstinctApi.HighlightResult.color(targetRole.color());
            }
            if (HackerComponent.KEY.get(targetPlayer).hackingTime >= HackerConstants.HACKING_TIME_TICKS) {
                return InstinctApi.HighlightResult.color(Color.GREEN.getRGB());
            }
            return InstinctApi.HighlightResult.color(NoellesRoleRegistry.HACKER.color());
        });
    }
}
