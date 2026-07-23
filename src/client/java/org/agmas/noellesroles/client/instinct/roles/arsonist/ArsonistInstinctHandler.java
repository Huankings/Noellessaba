package org.agmas.noellesroles.client.instinct.roles.arsonist;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.arsonist.DousedPlayerComponent;

import java.awt.Color;

/**
 * 纵火犯自己的本能视角。
 */
public final class ArsonistInstinctHandler {
    private ArsonistInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("arsonist_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.ARSONIST)
                    && WatheClient.isInstinctInputActive()) {
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("arsonist_targets"), NoellesInstinctHandlers.PRIORITY_ROLE_INSTINCT_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!(target instanceof PlayerEntity targetPlayer)
                    || !gameWorld.isRole(viewer, Noellesroles.ARSONIST)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 纵火犯开本能后按浇油状态染色：已浇油为职业色，未浇油为灰色。
             * 这份数据来自服务端同步的 DousedPlayerComponent，避免客户端自行猜测。
             */
            DousedPlayerComponent doused = DousedPlayerComponent.KEY.get(targetPlayer);
            return InstinctApi.HighlightResult.color(doused.isDoused() ? Noellesroles.ARSONIST.color() : Color.GRAY.getRGB());
        });
    }
}
