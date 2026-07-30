package org.agmas.noellesroles.client.instinct.roles.morphling;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.morphling.MorphMarkPlayerComponent;
import org.agmas.noellesroles.roles.morphling.MorphlingConstants;

/**
 * 变形怪试剂标记的客户端高亮。
 */
public final class MorphlingInstinctHandler {
    private MorphlingInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("morphling/reagent_mark"),
                MorphlingConstants.MARK_HIGHLIGHT_PRIORITY,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity targetPlayer)
                            || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                            || !WatheClient.isPlayerAliveAndInSurvival()) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    if (WatheClient.isInstinctEnabled()) {
                        /*
                         * 试剂标记是变形怪自己的布点提示，不是杀手本能颜色。
                         * 玩家主动开本能时交回 Wathe 默认杀手本能，避免标记色覆盖其它阵营信息。
                         */
                        return InstinctApi.HighlightResult.pass();
                    }

                    if (!GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.MORPHLING)) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    MorphMarkPlayerComponent component = MorphMarkPlayerComponent.KEY.get(targetPlayer);
                    return component.isMarkedBy(viewer.getUuid())
                            ? InstinctApi.HighlightResult.color(NoellesRoleRegistry.MORPHLING.color())
                            : InstinctApi.HighlightResult.pass();
                }
        );
    }
}
