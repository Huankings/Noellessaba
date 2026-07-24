package org.agmas.noellesroles.client.instinct.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.client.roles.convener.ConvenerColorHelper;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;

/**
 * 召集者本能与召集后的本能压制。
 */
public final class ConvenerInstinctHandler {
    private ConvenerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("convener_disguise_suppression"), NoellesInstinctHandlers.PRIORITY_CONVENER_SUPPRESSION, viewer -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(viewer);
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && disguise.getMorphTicks() > 0
                    && !gameWorld.isRole(viewer, NoellesRoleRegistry.CONVENER)) {
                /*
                 * 被召集活人在限时伪装期间失去“按本能键开启”的透视资格。
                 * 这里只返回 DISABLE，不改 Cook/Angel 等不依赖本能开关的独立标记。
                 */
                return InstinctApi.AvailabilityResult.DISABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("convener_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.CONVENER)
                    && WatheClient.isInstinctInputActive()) {
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("convener_targets"), NoellesInstinctHandlers.PRIORITY_CONVENER_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!gameWorld.isRole(viewer, NoellesRoleRegistry.CONVENER)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }

            if (target instanceof PlayerBodyEntity) {
                return InstinctApi.HighlightResult.color(NoellesRoleRegistry.CONVENER.color());
            }
            if (target instanceof PlayerEntity targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                /*
                 * 活人用流动色统一覆盖，避免召集者本能泄露目标原本阵营颜色。
                 */
                return InstinctApi.HighlightResult.color(ConvenerColorHelper.getPlayerFlowColor(targetPlayer.getUuid()));
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
