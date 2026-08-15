package org.agmas.noellesroles.client.instinct.roles.jason;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.agmas.noellesroles.roles.jason.JasonWoundedPlayerComponent;

/**
 * 杰森对汽油目标的橙色本能透视。
 */
public final class JasonInstinctHandler {
    private JasonInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("jason_ability_suppression"),
                JasonConstants.ABILITY_INSTINCT_SUPPRESSION_PRIORITY,
                (viewer, target) -> {
                    /*
                     * 无恶不在期间，任何存活玩家都不能通过杀手本能或扩展被动透视看到杰森。
                     * 旁观/创造调试视角放行，避免复盘或管理观察被硬隐藏。
                     */
                    if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                            && target instanceof PlayerEntity targetPlayer
                            && JasonAbilityRules.isAbilityActiveLike(targetPlayer)) {
                        return InstinctApi.HighlightResult.hide();
                    }
                    return InstinctApi.HighlightResult.pass();
                }
        );

        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("jason_gasoline"),
                NoellesInstinctHandlers.PRIORITY_ABILITY_MARK,
                (viewer, target) -> {
                    /*
                     * 标记只服务于仍在局内存活的杰森。旁观/创造玩家不进入这一分支，
                     * 避免组件同步残留让非存活玩家获得额外可用信息。
                     */
                    if (!GameFunctions.isPlayerAliveAndSurvival(viewer)
                            || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.JASON)
                            || !(target instanceof PlayerEntity targetPlayer)
                            || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                            || !JasonWoundedPlayerComponent.KEY.get(targetPlayer).isGasoline()) {
                        return InstinctApi.HighlightResult.pass();
                    }
                    /*
                     * 汽油橙色透视是杰森油桶的被动信息，不应该压过 Wathe 原生杀手本能。
                     * 当本能键已经开启时主动让路，让后续的默认杀手本能高亮接管目标颜色。
                     */
                    if (WatheClient.isInstinctEnabled()) {
                        return InstinctApi.HighlightResult.pass();
                    }
                    return InstinctApi.HighlightResult.color(JasonConstants.GASOLINE_INSTINCT_COLOR);
                }
        );
    }
}
