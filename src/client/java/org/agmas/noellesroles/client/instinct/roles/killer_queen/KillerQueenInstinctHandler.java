package org.agmas.noellesroles.client.instinct.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.agmas.noellesroles.client.instinct.NoellesInstinctHandlers;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenConstants;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenPsychoHandler;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenRules;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenWorldComponent;

/** 杀手皇后的主动本能、标记被动透视、招募目标透视和疯魔反透视。 */
public final class KillerQueenInstinctHandler {
    private KillerQueenInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(NoellesInstinctHandlers.id("killer_queen_availability"), KillerQueenConstants.INSTINCT_AVAILABILITY_PRIORITY, viewer ->
                GameFunctions.isPlayerAliveAndSurvival(viewer)
                        && KillerQueenRules.isKillerQueen(viewer)
                        && WatheClient.isInstinctInputActive()
                        ? InstinctApi.AvailabilityResult.ENABLE : InstinctApi.AvailabilityResult.PASS);

        /* 疯魔杀手皇后对好人/独立中立本能完全隐藏，优先级必须高于所有职业颜色。 */
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("killer_queen_psycho_hide"), KillerQueenConstants.PSYCHO_INSTINCT_HIDE_PRIORITY, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || !WatheClient.isInstinctEnabled()
                    || !KillerQueenRules.isLivingGoodOrIndependent(viewer)) return InstinctApi.HighlightResult.pass();
            return PsychoModeApi.isActive(targetPlayer, KillerQueenPsychoHandler.PROFILE_ID)
                    ? InstinctApi.HighlightResult.hide() : InstinctApi.HighlightResult.pass();
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("killer_queen_instinct"), KillerQueenConstants.ACTIVE_INSTINCT_COLOR_PRIORITY, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || !KillerQueenRules.isKillerQueen(viewer)
                    || !WatheClient.isInstinctEnabled() || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }
            var role = GameWorldComponent.KEY.get(viewer.getWorld()).getRole(targetPlayer);
            if (role == null) return InstinctApi.HighlightResult.pass();
            /*
             * 主动本能开启时，已被“当前这名杀手皇后”标记的目标优先显示独立紫色。
             * 这条分支必须先于 Mimic 和阵营颜色，否则职业色会再次掩盖标记状态。
             */
            if (KillerQueenWorldComponent.KEY.get(viewer.getWorld())
                    .isMarkedBy(viewer.getUuid(), targetPlayer.getUuid())) {
                return InstinctApi.HighlightResult.color(KillerQueenConstants.MARKED_INSTINCT_COLOR);
            }
            if (role == NoellesRoleRegistry.MIMIC) return InstinctApi.HighlightResult.color(role.color());
            if (role.getFaction() == Faction.KILLER) {
                return InstinctApi.HighlightResult.color(MathHelper.hsvToRgb(0.0F, 1.0F, 0.6F));
            }
            /*
             * Wathe 的 NEUTRAL 同时包含普通中立、杀手中立和独立中立，不能整体显示目标职业色。
             * 只有前两组暴露目标职业色；独立中立与好人统一显示杀手皇后职业色。
             * 上方 Mimic 的原有特殊颜色分支仍优先保留。
             */
            if (NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(role)
                    || NoellesRoleGroups.ORDINARY_NEUTRALS.contains(role)) {
                return InstinctApi.HighlightResult.color(role.color());
            }
            return InstinctApi.HighlightResult.color(KillerQueenConstants.ROLE_COLOR);
        });

        /* 标记是常驻被动信息，但优先级低于上面的主动本能，避免覆盖主动本能阵营色。 */
        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("killer_queen_mark"), KillerQueenConstants.MARK_PASSIVE_HIGHLIGHT_PRIORITY, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer) || !KillerQueenRules.isKillerQueen(viewer)
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !KillerQueenWorldComponent.KEY.get(viewer.getWorld()).isMarkedBy(viewer.getUuid(), targetPlayer.getUuid())) {
                return InstinctApi.HighlightResult.pass();
            }
            return InstinctApi.HighlightResult.color(KillerQueenConstants.ROLE_COLOR);
        });

        InstinctApi.registerHighlight(NoellesInstinctHandlers.id("killer_queen_recruit_target"), KillerQueenConstants.RECRUIT_TARGET_HIGHLIGHT_PRIORITY, (viewer, target) -> {
            if (!(target instanceof PlayerEntity targetPlayer)) return InstinctApi.HighlightResult.pass();
            var recruitment = KillerQueenWorldComponent.KEY.get(viewer.getWorld()).getRecruitment(viewer.getUuid());
            return recruitment != null && recruitment.target().equals(targetPlayer.getUuid())
                    ? InstinctApi.HighlightResult.color(KillerQueenConstants.ROLE_COLOR)
                    : InstinctApi.HighlightResult.pass();
        });
    }
}
