package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.harpymodloader.api.assignment.ModifierAssignmentApi;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentApi;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhase;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhaseContext;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 影子小丑的 Harpy 分配规则。
 *
 * <p>随机生成时只允许 Harpy 抽到一个影子小丑，第二个在平民替换阶段结束后从仍是平民阵营的玩家里补齐。
 * 强制指令也在同一阶段消费，这样最终写入职业仍走 Harpy 的公开分配事件链。</p>
 */
public final class ShadowJesterRoleAssignmentRules {
    private static boolean initialized = false;

    private ShadowJesterRoleAssignmentRules() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        RoleAssignmentApi.registerAfterPhaseHandler(
                NoellesRolesCore.id("shadow_jester_pair_assignment"),
                RoleAssignmentPhase.CIVILIAN_REPLACEMENT,
                0,
                ShadowJesterRoleAssignmentRules::assignShadowJesterPair
        );
        ModifierAssignmentApi.registerModifierExcludesRole(
                NoellesRolesCore.id("shadow_jester_excludes_lovers"),
                100,
                NoellesModifierRegistry.LOVERS,
                NoellesRoleRegistry.SHADOW_JESTER
        );
        ModifierAssignmentApi.registerModifierExcludesRole(
                NoellesRolesCore.id("shadow_jester_excludes_dual_personality"),
                100,
                NoellesModifierRegistry.DUAL_PERSONALITY,
                NoellesRoleRegistry.SHADOW_JESTER
        );
    }

    private static void assignShadowJesterPair(RoleAssignmentPhaseContext context) {
        ForcedShadowJesterManager.ApplyResult forcedResult = ForcedShadowJesterManager.consumeAndApplyPendingPair(context);
        if (forcedResult.changedAnything()) {
            NoellesRolesCore.LOGGER.info(
                    "已应用强制影子小丑配对：成功 {} 对，作废或未启用 {} 对。",
                    forcedResult.appliedPairs(),
                    forcedResult.skippedPairs()
            );
        }
        if (forcedResult.appliedPairs() > 0) {
            return;
        }

        GameWorldComponent gameWorld = context.gameWorldComponent();
        List<ServerPlayerEntity> shadowJesters = context.players().stream()
                .filter(player -> gameWorld.isRole(player, NoellesRoleRegistry.SHADOW_JESTER))
                .toList();
        if (shadowJesters.size() != 1) {
            if (shadowJesters.isEmpty()) {
                ShadowJesterComponent.KEY.get(context.serverWorld()).clear();
            }
            return;
        }

        ServerPlayerEntity first = shadowJesters.getFirst();
        List<ServerPlayerEntity> candidates = collectSecondPartnerCandidates(context, first, true);
        if (candidates.isEmpty()) {
            /*
             * 正常情况下优先从“仍是原版平民”的玩家里补第二名。
             * 这样不会把已经拿到扩展平民开局道具的人再覆盖成影子小丑。
             * 如果人数/配置导致没有原版平民，再退回到任意平民阵营候选，并依赖
             * ShadowJesterRoleAssignedHandler 的开局物品清理兜底。
             */
            candidates = collectSecondPartnerCandidates(context, first, false);
        }
        Collections.shuffle(candidates);
        if (candidates.isEmpty()) {
            NoellesRolesCore.LOGGER.warn("影子小丑已随机到 1 人，但没有可用平民候选者补齐第二名。");
            return;
        }

        ServerPlayerEntity second = candidates.getFirst();
        context.assignRole(second, NoellesRoleRegistry.SHADOW_JESTER);
        ShadowJesterComponent.KEY.get(context.serverWorld()).setPair(first.getUuid(), second.getUuid(), false);
        ShadowJesterTaskHandler.prepareInitialTasks(first);
        ShadowJesterTaskHandler.prepareInitialTasks(second);
    }

    private static List<ServerPlayerEntity> collectSecondPartnerCandidates(
            RoleAssignmentPhaseContext context,
            ServerPlayerEntity first,
            boolean vanillaCivilianOnly
    ) {
        GameWorldComponent gameWorld = context.gameWorldComponent();
        List<ServerPlayerEntity> candidates = new ArrayList<>(context.players());
        candidates.removeIf(player -> {
            if (player.getUuid().equals(first.getUuid())) {
                return true;
            }
            Role role = gameWorld.getRole(player);
            if (role == null) {
                return true;
            }
            if (vanillaCivilianOnly) {
                return role != WatheRoles.CIVILIAN;
            }
            return role.getFaction() != Faction.CIVILIAN;
        });
        return candidates;
    }
}
