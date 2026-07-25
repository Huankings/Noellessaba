package org.agmas.noellesroles.client.instinct.roles.bounty_hunter;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterConstants;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;

/**
 * 赏金猎人的悬赏目标高亮。
 */
public final class BountyHunterInstinctHandler {
    private BountyHunterInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("bounty_hunter_target"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.BOUNTY_HUNTER)) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 悬赏目标是职业被动信息，不依赖玩家是否按下本能键；
             * 但它仍然只给存活赏金猎人显示，死亡后交回观察者/其他职业自己的高亮规则。
             */
            BountyHunterPlayerComponent bountyHunter = BountyHunterPlayerComponent.KEY.get(viewer);
            if (bountyHunter.isCurrentBountyTarget(targetPlayer)) {
                return InstinctApi.HighlightResult.color(BountyHunterConstants.ROLE_COLOR);
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
