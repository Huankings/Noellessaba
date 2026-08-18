package org.agmas.noellesroles.client.instinct.roles.shadow_jester;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterPhase;

import java.awt.Color;

/**
 * 影子小丑的伴侣透视和第三阶段本能透视。
 */
public final class ShadowJesterInstinctHandler {
    private ShadowJesterInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(
                NoellesInstinctHandlers.id("shadow_jester_availability"),
                InstinctApi.DEFAULT_PRIORITY,
                viewer -> {
                    ShadowJesterComponent component = ShadowJesterComponent.KEY.get(viewer.getWorld());
                    if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                            && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.SHADOW_JESTER)
                            && component.getPhase(viewer.getUuid()).atLeast(ShadowJesterPhase.VOW_BOUND)
                            && WatheClient.isInstinctInputActive()) {
                        /*
                         * 第三阶段后影子小丑才真正获得“按本能键查看全场”的能力。
                         * 第一/第二阶段只有另一半被动描边，不应该因为按本能键获得额外信息。
                         */
                        return InstinctApi.AvailabilityResult.ENABLE;
                    }
                    return InstinctApi.AvailabilityResult.PASS;
                }
        );

        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("shadow_jester_active_instinct"),
                NoellesInstinctHandlers.PRIORITY_ROLE_INSTINCT_COLOR,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity targetPlayer)
                            || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                            || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                            || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.SHADOW_JESTER)
                            || !WatheClient.isInstinctEnabled()) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    ShadowJesterComponent component = ShadowJesterComponent.KEY.get(viewer.getWorld());
                    if (!component.getPhase(viewer.getUuid()).atLeast(ShadowJesterPhase.VOW_BOUND)) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    /*
                     * 缔结后按本能键时：另一半保持职业色，其他玩家统一灰色。
                     * 这样既能保留绑定识别，也不会通过颜色泄露其它人的真实阵营。
                     */
                    return InstinctApi.HighlightResult.color(
                            component.arePartners(viewer.getUuid(), targetPlayer.getUuid())
                                    ? ShadowJesterConstants.ROLE_COLOR
                                    : Color.GRAY.getRGB()
                    );
                }
        );

        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("shadow_jester_passive_partner"),
                NoellesInstinctHandlers.PRIORITY_ABILITY_MARK,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity targetPlayer)
                            || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                            || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                            || !GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.SHADOW_JESTER)) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    /*
                     * 被动伴侣透视不依赖本能键；但玩家真正开启第三阶段本能时，
                     * 这条低信息量被动规则必须让位给上面的主动本能规则和 Wathe 默认本能链。
                     */
                    if (WatheClient.isInstinctEnabled()) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    ShadowJesterComponent component = ShadowJesterComponent.KEY.get(viewer.getWorld());
                    return component.arePartners(viewer.getUuid(), targetPlayer.getUuid())
                            ? InstinctApi.HighlightResult.color(ShadowJesterConstants.ROLE_COLOR)
                            : InstinctApi.HighlightResult.pass();
                }
        );
    }
}
