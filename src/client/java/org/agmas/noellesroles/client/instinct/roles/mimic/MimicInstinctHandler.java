package org.agmas.noellesroles.client.instinct.roles.mimic;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;

public final class MimicInstinctHandler {
    private MimicInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("mimic_to_killers"), NoellesInstinctHandlers.PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }
            if (!WatheClient.isInstinctEnabled() || !WatheClient.isKiller() || !WatheClient.isPlayerAliveAndInSurvival()) {
                return InstinctApi.HighlightResult.pass();
            }

            if (GameWorldComponent.KEY.get(viewer.getWorld()).isRole(targetPlayer, NoellesRoleRegistry.MIMIC)) {
                /*
                 * Mimic 虽然是好人阵营，但杀手本能看到它时要按红色杀手目标处理。
                 * 这是对 Wathe 默认颜色的覆盖，所以使用高 priority。
                 */
                return InstinctApi.HighlightResult.color(MathHelper.hsvToRgb(0.0F, 1.0F, 0.6F));
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
