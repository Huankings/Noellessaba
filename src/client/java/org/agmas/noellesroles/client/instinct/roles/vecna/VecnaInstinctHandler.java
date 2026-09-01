package org.agmas.noellesroles.client.instinct.roles.vecna;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.vecna.VecnaPlayerComponent;

/** 维克那对自己标记目标的被动透视颜色。 */
public final class VecnaInstinctHandler {
    private VecnaInstinctHandler() {}
    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("vecna_mark"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) return InstinctApi.HighlightResult.pass();
            if (viewer instanceof PlayerEntity viewerPlayer && viewerPlayer.getWorld() == targetPlayer.getWorld()
                    && viewerPlayer.getUuid().equals(VecnaPlayerComponent.KEY.get(targetPlayer).getMarker())
                    && VecnaPlayerComponent.KEY.get(targetPlayer).isMarked()) {
                return InstinctApi.HighlightResult.color(NoellesRoleRegistry.VECNA.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
