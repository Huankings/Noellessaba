package org.agmas.noellesroles.client.instinct.roles.corpsemaker;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.corpsemaker.CorpsemakerBodyComponent;

/** 给普通非存活观察者补齐“未参与本局玩家的造尸怪伪尸”白色本能描边。 */
public final class CorpsemakerInstinctHandler {
    private static final int NON_PARTICIPANT_BODY_COLOR = 0xFFFFFF;

    private CorpsemakerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("corpsemaker_non_participant_body"),
                NoellesInstinctHandlers.PRIORITY_ABILITY_MARK,
                (viewer, target) -> {
                    if (!(target instanceof PlayerBodyEntity body)
                            || !GameFunctions.isPlayerSpectatingOrCreative(viewer)
                            || !WatheClient.isInstinctEnabled()
                            || !CorpsemakerBodyComponent.KEY.get(body).shouldUseWhiteObserverInstinct()) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    /*
                     * 时间狭缝玩家带有 alive override，不满足 isPlayerSpectatingOrCreative，
                     * 因而仍保持局内存活视角；只有真正的普通观察者会得到这条白色描边。
                     */
                    return InstinctApi.HighlightResult.color(NON_PARTICIPANT_BODY_COLOR);
                }
        );
    }
}
