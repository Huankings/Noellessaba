package org.agmas.noellesroles.client.instinct.roles.physician;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;

import java.awt.Color;

/**
 * 医师看到真毒和幻觉目标。
 */
public final class PhysicianInstinctHandler {
    private PhysicianInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("physician_marks"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            boolean abnormal = PlayerPoisonComponent.KEY.get(targetPlayer).poisonTicks > 0
                    || DelusionPlayerComponent.KEY.get(targetPlayer).isActive();
            if (GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.PHYSICIAN)
                    && WatheClient.isPlayerAliveAndInSurvival()
                    && abnormal) {
                return InstinctApi.HighlightResult.color(Color.RED.getRGB());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
