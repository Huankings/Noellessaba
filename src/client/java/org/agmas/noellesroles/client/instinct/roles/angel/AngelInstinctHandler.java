package org.agmas.noellesroles.client.instinct.roles.angel;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;

public final class AngelInstinctHandler {
    private AngelInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("angel_guard_target"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }
            if (!GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.ANGEL)
                    || !WatheClient.isPlayerAliveAndInSurvival()) {
                return InstinctApi.HighlightResult.pass();
            }

            AngelPlayerComponent angel = AngelPlayerComponent.KEY.get(viewer);
            if (angel.getGuardedTarget() != null && angel.getGuardedTarget().equals(target.getUuid())) {
                /*
                 * 天使守护目标是职业选择信息，不属于“按本能键才开启”的本能链路。
                 * 所以它保持独立显示，避免被 Convener 的本能压制误关。
                 */
                return InstinctApi.HighlightResult.color(Noellesroles.ANGEL.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
