package org.agmas.noellesroles.client.instinct.roles.drugmaker;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;

import java.util.UUID;

/**
 * 制毒师对中毒玩家的被动高亮。
 */
public final class DrugmakerInstinctHandler {
    private static final UUID DELUSION_MARKER = UUID.fromString("00000000-0000-0000-dead-c0de00000000");

    private DrugmakerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("drugmaker_marks"),
                NoellesInstinctHandlers.PRIORITY_ABILITY_MARK,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity targetPlayer) || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    PlayerPoisonComponent targetPoison = PlayerPoisonComponent.KEY.get(targetPlayer);
                    if (GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.DRUGMAKER)
                            && WatheClient.isPlayerAliveAndInSurvival()
                            && !WatheClient.isInstinctEnabled()
                            && targetPoison.poisonTicks > 0
                            && !(targetPoison.poisoner != null && targetPoison.poisoner.equals(DELUSION_MARKER))) {
                        /*
                         * 只在未开启本能时显示，避免覆盖 Wathe 自己的杀手本能颜色。
                         * 幻觉 marker 明确排除，避免把幻觉试剂误显示成真实中毒。
                         */
                        return InstinctApi.HighlightResult.color(DrugmakerConstants.ROLE_COLOR);
                    }
                    return InstinctApi.HighlightResult.pass();
                }
        );
    }
}
