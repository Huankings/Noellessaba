package org.agmas.noellesroles.client.instinct.roles.licensed_villain;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 执照恶棍本能透视。
 */
public final class LicensedVillainInstinctHandler {
    private LicensedVillainInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("licensed_villain_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.LICENSED_VILLAIN)
                    && WatheClient.isInstinctInputActive()) {
                /*
                 * 执照恶棍本能只在本人仍存活时开启。
                 * 死亡观察者保留职业身份时，仍应交给观察者本能规则，而不是继续套本职业颜色。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("licensed_villain_targets"), NoellesInstinctHandlers.PRIORITY_ROLE_INSTINCT_COLOR, (viewer, target) -> {
            if (target instanceof PlayerEntity targetPlayer
                    && GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                    && GameWorldComponent.KEY.get(viewer.getWorld()).isRole(viewer, NoellesRoleRegistry.LICENSED_VILLAIN)
                    && WatheClient.isInstinctEnabled()) {
                /*
                 * 执照恶棍是独立中立杀戮角色，本能开启时所有存活玩家统一显示为自己的职业色。
                 */
                return InstinctApi.HighlightResult.color(NoellesRoleRegistry.LICENSED_VILLAIN.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
