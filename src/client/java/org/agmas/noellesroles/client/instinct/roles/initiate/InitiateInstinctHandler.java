package org.agmas.noellesroles.client.instinct.roles.initiate;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

public final class InitiateInstinctHandler {
    private InitiateInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("initiate_targets"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (gameWorld.isRole(targetPlayer, NoellesRoleRegistry.INITIATE)
                    && gameWorld.isRole(viewer, NoellesRoleRegistry.INITIATE)) {
                /*
                 * 初学者互相识别不依赖本能键，是职业关系提示。
                 * 但仍然只对存活初学者开放；死亡后不能用这条高优先级标记覆盖观察者职业色。
                 */
                return InstinctApi.HighlightResult.color(NoellesRoleRegistry.INITIATE.color());
            }
            if (gameWorld.isRole(targetPlayer, NoellesRoleRegistry.INITIATE)
                    && WatheClient.isInstinctEnabled()
                    && gameWorld.canUseKillerFeatures(viewer)) {
                /*
                 * 杀手通过本能识别初学者时，仍必须依赖 isInstinctEnabled()。
                 */
                return InstinctApi.HighlightResult.color(NoellesRoleRegistry.INITIATE.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
