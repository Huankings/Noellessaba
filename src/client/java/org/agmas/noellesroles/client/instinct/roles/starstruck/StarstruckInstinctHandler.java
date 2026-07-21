package org.agmas.noellesroles.client.instinct.roles.starstruck;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.starstruck.StarstruckPlayerComponent;

/**
 * 星界使者能力期间临时开启本能。
 */
public final class StarstruckInstinctHandler {
    private StarstruckInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("starstruck"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && gameWorld.isRole(viewer, Noellesroles.STARSTRUCK)
                    && StarstruckPlayerComponent.KEY.get(viewer).ticks > 0) {
                /*
                 * 这里仅给星界使者本人开启本能资格。
                 * 高亮颜色单独注册，避免其它职业或扩展关闭本能时被颜色逻辑绕开。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("starstruck_color"), NoellesInstinctHandlers.PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && gameWorld.isRole(viewer, Noellesroles.STARSTRUCK)
                    && StarstruckPlayerComponent.KEY.get(viewer).ticks > 0
                    && WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.color(Noellesroles.STARSTRUCK.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
