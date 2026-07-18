package org.agmas.noellesroles.client.instinct.roles.executioner;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.roles.executioner.ExecutionerPlayerComponent;

import java.awt.Color;

public final class ExecutionerInstinctHandler {
    private ExecutionerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("executioner_target"), NoellesInstinctHandlers.PRIORITY_ABILITY_MARK, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.EXECUTIONER)) {
                return InstinctApi.HighlightResult.pass();
            }

            ExecutionerPlayerComponent executioner = ExecutionerPlayerComponent.KEY.get(viewer);
            if (executioner.target != null && executioner.target.equals(target.getUuid())) {
                /*
                 * 处刑人目标黄框是目标锁定信息，不依赖本能键。
                 * 但它仍然只属于存活处刑人的职业信息；死亡后应交给观察者职业色透视。
                 * 它和处刑人“开本能后全员职业色”是两套不同语义，所以拆在同一个职业类的两个注册点里。
                 */
                return InstinctApi.HighlightResult.color(Color.YELLOW.getRGB());
            }
            return InstinctApi.HighlightResult.pass();
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("executioner_instinct_color"), InstinctApi.DEFAULT_PRIORITY, (viewer, target) -> {
            if (target instanceof PlayerEntity targetPlayer
                    && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, Noellesroles.EXECUTIONER)
                    && WatheClient.isPlayerAliveAndInSurvival()
                    && WatheClient.isInstinctEnabled()) {
                /*
                 * 处刑人主动本能颜色仍依赖 WatheClient.isInstinctEnabled()。
                 * 这保证它会被 Convener 变形压制等 availability DISABLE 规则统一关掉。
                 */
                return InstinctApi.HighlightResult.color(Noellesroles.EXECUTIONER.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
