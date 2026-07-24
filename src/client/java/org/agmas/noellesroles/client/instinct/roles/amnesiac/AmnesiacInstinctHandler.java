package org.agmas.noellesroles.client.instinct.roles.amnesiac;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.amnesiac.AmnesiacConstants;

/**
 * 失忆患者本能提示。
 */
public final class AmnesiacInstinctHandler {
    private AmnesiacInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("amnesiac_to_killers"), NoellesInstinctHandlers.PRIORITY_SPECIAL_NEUTRAL_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (!AmnesiacConstants.AMNESIAC_GLOWS_DIFFERENTLY
                    || !(target instanceof PlayerEntity targetPlayer)
                    || !gameWorld.isRole(targetPlayer, NoellesRoleRegistry.AMNESIAC)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()
                    || !gameWorld.canUseKillerFeatures(viewer)) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 这只是杀手本能的特殊颜色覆盖，不代表失忆患者被加入杀手侧中立池。
             * 因此它依赖 WatheClient.isInstinctEnabled()，可被召集者的本能压制规则关闭。
             */
            return InstinctApi.HighlightResult.color(NoellesRoleRegistry.AMNESIAC.color());
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("amnesiac_bodies"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!AmnesiacConstants.BODIES_GLOW_TO_AMNESIAC
                    || !(target instanceof PlayerBodyEntity)
                    || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.AMNESIAC)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 尸体高亮是失忆患者自身的交互提示，不依赖本能键。
             * 这样召集者只压制“按本能键开启”的阵营透视时，不会误关掉尸体可交互提示。
             */
            return InstinctApi.HighlightResult.color(NoellesRoleRegistry.AMNESIAC.color());
        });
    }
}
