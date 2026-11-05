package org.agmas.noellesroles.client.instinct.killer;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.harpymodloader.api.neutral.NeutralRoleCategoryApi;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;

/**
 * NoellesRoles 自己保留的“杀手看其它中立”固定颜色规则。
 *
 * <p>杀手中立的真实职业色已经迁到 Harpy 公共分类能力；这里只处理普通中立、独立中立
 * 以及尚未注册细分类的中立职业，保持 NoellesRoles 原有的固定中立色玩法。</p>
 */
public final class KillerOtherNeutralInstinctHandler {
    private KillerOtherNeutralInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("killer_other_neutral_targets"),
                NoellesInstinctHandlers.PRIORITY_HIGH_INSTINCT_COLOR,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity targetPlayer)
                            || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                            || !WatheClient.isInstinctEnabled()
                            || !WatheClient.isKiller()
                            || !WatheClient.isPlayerAliveAndInSurvival()) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
                    Role role = gameWorld.getRole(targetPlayer);
                    if (role == null || role.getFaction() != Faction.NEUTRAL
                            || NeutralRoleCategoryApi.isKillerNeutral(role)) {
                        return InstinctApi.HighlightResult.pass();
                    }
                    return InstinctApi.HighlightResult.color(5168437);
                }
        );
    }
}
