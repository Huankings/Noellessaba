package org.agmas.noellesroles.client.instinct.roles.magician;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.entities.MagicianPlaybackEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 魔术师本人和普通非存活观察者对播放皮套的主动本能描边。 */
public final class MagicianInstinctHandler {
    private MagicianInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("magician_playback"),
                NoellesInstinctHandlers.PRIORITY_ROLE_INSTINCT_COLOR,
                (viewer, target) -> {
                    if (!(target instanceof MagicianPlaybackEntity) || !WatheClient.isInstinctEnabled()) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    boolean aliveMagician = GameFunctions.isPlayerAliveAndSurvival(viewer)
                            && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.MAGICIAN);
                    boolean ordinaryObserver = GameFunctions.isPlayerSpectatingOrCreative(viewer);
                    if (!aliveMagician && !ordinaryObserver) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    /*
                     * 时间狭缝旁观具有玩法存活授权：若本人不是魔术师，就不会借原版 spectator 模式
                     * 获得观察者透视；普通死亡旁观则通过 isPlayerSpectatingOrCreative 明确放行。
                     */
                    return InstinctApi.HighlightResult.color(NoellesRoleRegistry.MAGICIAN.color());
                }
        );
    }
}
