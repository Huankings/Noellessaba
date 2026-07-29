package org.agmas.noellesroles.client.instinct.roles.initiate;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.initiate.InitiateConstants;

public final class InitiateInstinctHandler {
    private InitiateInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("initiate_to_killers"), NoellesInstinctHandlers.PRIORITY_SPECIAL_NEUTRAL_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            if (gameWorld.isRole(targetPlayer, NoellesRoleRegistry.INITIATE)
                    && WatheClient.isInstinctEnabled()
                    && gameWorld.canUseKillerFeatures(viewer)) {
                /*
                 * 初学者是普通中立，通用 KillerNeutralInstinctHandler 会在较低优先级
                 * 把普通中立染成固定绿色。这里使用“特殊中立职业色”优先级先接住初学者，
                 * 否则同优先级且后注册的通用规则会提前返回，杀手就永远看不到初学者职业色。
                 *
                 * 关闭常量时必须返回 hide() 而不是 pass()：pass() 会继续落到通用中立绿色兜底，
                 * 导致配置明明关闭，杀手本能仍能看到初学者的中立描边。
                 */
                if (!InitiateConstants.GLOWS_TO_KILLER_INSTINCT) {
                    return InstinctApi.HighlightResult.hide();
                }
                return InstinctApi.HighlightResult.color(NoellesRoleRegistry.INITIATE.color());
            }
            return InstinctApi.HighlightResult.pass();
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("initiate_partners"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
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
                 * 这条仍保留在能力标记优先级，避免为了修杀手本能颜色而意外压过其它更高层级的透视规则。
                 */
                return InstinctApi.HighlightResult.color(NoellesRoleRegistry.INITIATE.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
