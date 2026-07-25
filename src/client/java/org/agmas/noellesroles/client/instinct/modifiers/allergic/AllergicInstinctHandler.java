package org.agmas.noellesroles.client.instinct.modifiers.allergic;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.modifiers.allergic.AllergicConstants;
import org.agmas.noellesroles.modifiers.allergic.AllergicPlayerComponent;

/**
 * 过敏患者触发本能后的临时透视。
 */
public final class AllergicInstinctHandler {
    private AllergicInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("allergic"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            AllergicPlayerComponent allergic = AllergicPlayerComponent.KEY.get(viewer);
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && allergic.isAllergic()
                    && allergic.hasAllergicInstinct()) {
                /*
                 * 这里用 Wathe 的 availability API 开启本能资格，
                 * 让 Convener 等更高优先级的“禁用本能”规则仍能统一压制过敏透视。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("allergic_color"), NoellesInstinctHandlers.PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            AllergicPlayerComponent allergic = AllergicPlayerComponent.KEY.get(viewer);
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && allergic.isAllergic()
                    && allergic.hasAllergicInstinct()
                    && WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.color(AllergicConstants.COLOR);
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
