package org.agmas.noellesroles.client.instinct.roles.spring_trap;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapPsychoHandler;

/**
 * 弹簧陷阱状态下对敌方本能隐藏。
 */
public final class SpringTrapInstinctHandler {
    private SpringTrapInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("spring_trap_psycho_hide"),
                NoellesInstinctHandlers.PRIORITY_TIMEKEEPER_RIFT_SUPPRESSION - 100,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity targetPlayer) || !SpringTrapPsychoHandler.isSpringTrapActive(targetPlayer)) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
                    var viewerRole = gameWorld.getRole(viewer);
                    if (viewerRole == null) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    boolean shouldHide = viewerRole.getFaction() == Faction.CIVILIAN
                            || viewerRole.getFaction() == Faction.VIGILANTE
                            || NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(viewerRole);
                    return shouldHide ? InstinctApi.HighlightResult.hide() : InstinctApi.HighlightResult.pass();
                }
        );
    }
}
