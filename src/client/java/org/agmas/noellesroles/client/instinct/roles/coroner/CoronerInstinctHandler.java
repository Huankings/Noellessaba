package org.agmas.noellesroles.client.instinct.roles.coroner;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import org.agmas.noellesroles.ConfigWorldComponent;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 验尸官的尸体提示。
 * 只有存活且心情不低于中等阈值的验尸官会看到灰色描边，默认关闭。
 */
public final class CoronerInstinctHandler {
    private CoronerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("coroner_bodies"),
                NoellesInstinctHandlers.PRIORITY_ABILITY_MARK,
                (viewer, target) -> {
                    if (!(target instanceof PlayerBodyEntity)
                            || !ConfigWorldComponent.KEY.get(viewer.getWorld()).coronerBodyInstinct
                            || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.CORONER)
                            || !WatheClient.isPlayerAliveAndInSurvival()
                            || WatheClient.moodComponent == null) {
                        return InstinctApi.HighlightResult.pass();
                    }
                    PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(viewer);
                    if (mood != null && !mood.isLowerThanMid()) {
                        return InstinctApi.HighlightResult.color(0x606060);
                    }
                    return InstinctApi.HighlightResult.pass();
                }
        );
    }
}
