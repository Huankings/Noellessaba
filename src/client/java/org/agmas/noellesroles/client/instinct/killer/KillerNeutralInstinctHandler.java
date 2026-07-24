package org.agmas.noellesroles.client.instinct.killer;

import org.agmas.noellesroles.registry.NoellesRoleGroups;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;

public final class KillerNeutralInstinctHandler {
    private KillerNeutralInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("killer_neutral_targets"), NoellesInstinctHandlers.PRIORITY_HIGH_INSTINCT_COLOR, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || GameFunctions.isPlayerSpectatingOrCreative(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }
            if (!WatheClient.isInstinctEnabled() || !WatheClient.isKiller() || !WatheClient.isPlayerAliveAndInSurvival()) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
            Role role = gameWorld.getRole(targetPlayer);
            if (role == null) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * NoellesRoles 把 Jester/Executioner/Vulture 等列入“杀手侧中立”。
             * 杀手开本能看这些目标时要显示职业色；其它普通中立则使用旧逻辑里的固定中立色。
             */
            if (NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(role)) {
                return InstinctApi.HighlightResult.color(role.color());
            }
            if (!role.isInnocent() && !role.canUseKiller()) {
                return InstinctApi.HighlightResult.color(5168437);
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
