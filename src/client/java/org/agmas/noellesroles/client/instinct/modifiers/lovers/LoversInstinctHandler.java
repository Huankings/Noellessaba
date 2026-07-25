package org.agmas.noellesroles.client.instinct.modifiers.lovers;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.modifiers.lovers.LoversConstants;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;

public final class LoversInstinctHandler {
    private LoversInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("lovers_partner"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!LoversConstants.GLOW_TO_EACH_OTHER
                    || !(target instanceof PlayerEntity potentialLover)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
                return InstinctApi.HighlightResult.pass();
            }

            WorldModifierComponent component = WorldModifierComponent.KEY.get(viewer.getWorld());
            LoversPairComponent pairComponent = LoversPairComponent.KEY.get(viewer.getWorld());
            if (!component.isModifier(viewer, NoellesModifierRegistry.LOVERS)
                    || !component.isModifier(potentialLover, NoellesModifierRegistry.LOVERS)
                    || !pairComponent.arePartnersOrFallback(
                    viewer.getUuid(),
                    potentialLover.getUuid(),
                    component.getAllWithModifier(NoellesModifierRegistry.LOVERS)
            )) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 恋人词条只让“自己的伴侣”发光。
             * 多对恋人同时存在时，其他恋人对不会被暴露给当前玩家。
             */
            return InstinctApi.HighlightResult.color(LoversConstants.COLOR);
        });
    }
}
