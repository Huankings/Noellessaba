package org.agmas.noellesroles.client.instinct.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.roles.insane_damned_paranoid_killer.InsaneDamnedKillerPlayerComponent;

/**
 * 亡语杀手尸体伪装期间的本能透视隐藏。
 *
 * <p>尸体伪装不是隐身：玩家实体仍然会渲染成一具“尸体”。但如果好人、义警或独立中立
 * 开启本能后能看到描边，就会直接暴露这具尸体其实是活人。这里通过 Wathe 的 InstinctApi
 * 在描边结算阶段返回 hide，不新增渲染 mixin，也不影响杀手阵营 / 杀手侧中立的本能信息。</p>
 */
public final class InsaneDamnedKillerInstinctHandler {
    private InsaneDamnedKillerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(
                NoellesInstinctHandlers.id("insane_damned_killer/corpse_disguise_hide"),
                NoellesInstinctHandlers.PRIORITY_CORPSE_DISGUISE_SUPPRESSION,
                (viewer, target) -> {
                    if (!(target instanceof PlayerEntity targetPlayer)
                            || !InsaneDamnedKillerPlayerComponent.isActiveCorpseMode(targetPlayer)) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.getWorld());
                    Role viewerRole = gameWorld.getRole(viewer);
                    if (viewerRole == null) {
                        return InstinctApi.HighlightResult.pass();
                    }

                    /*
                     * 需求只要求对好人阵营、义警阵营和独立中立阵营隐藏本能描边。
                     * 杀手和杀手侧中立仍交给原有本能规则处理，避免误削队友 / 杀手阵营的信息能力。
                     */
                    boolean shouldHide = viewerRole.getFaction() == Faction.CIVILIAN
                            || viewerRole.getFaction() == Faction.VIGILANTE
                            || NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(viewerRole);
                    return shouldHide ? InstinctApi.HighlightResult.hide() : InstinctApi.HighlightResult.pass();
                }
        );
    }
}
