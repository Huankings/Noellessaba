package org.agmas.noellesroles.client.instinct.roles.party_animal;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.party_animal.HeliumBuzzPlayerComponent;
import org.agmas.noellesroles.roles.party_animal.PartyAnimalConstants;
import dev.doctor4t.wathe.cca.GameWorldComponent;

/** 派对狂查看变声目标的被动透视颜色。 */
public final class PartyAnimalInstinctHandler {
    private PartyAnimalInstinctHandler() {}

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("party_animal_pitch"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity player)
                            || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                            || !GameFunctions.isPlayerAliveAndSurvival(player)
                            || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.PARTY_ANIMAL)
                            || WatheClient.isInstinctEnabled()) return InstinctApi.HighlightResult.pass();
                    return color(HeliumBuzzPlayerComponent.KEY.get(player).getEffectiveLevel());
                });
    }

    private static InstinctApi.HighlightResult color(int level) {
        return switch (level) {
            case 1 -> InstinctApi.HighlightResult.color(PartyAnimalConstants.PITCH_UP_COLOR_1);
            case 2 -> InstinctApi.HighlightResult.color(PartyAnimalConstants.PITCH_UP_COLOR_2);
            case 3 -> InstinctApi.HighlightResult.color(PartyAnimalConstants.PITCH_UP_COLOR_3);
            case -1 -> InstinctApi.HighlightResult.color(PartyAnimalConstants.PITCH_DOWN_COLOR_1);
            case -2 -> InstinctApi.HighlightResult.color(PartyAnimalConstants.PITCH_DOWN_COLOR_2);
            case -3 -> InstinctApi.HighlightResult.color(PartyAnimalConstants.PITCH_DOWN_COLOR_3);
            default -> InstinctApi.HighlightResult.pass();
        };
    }
}
