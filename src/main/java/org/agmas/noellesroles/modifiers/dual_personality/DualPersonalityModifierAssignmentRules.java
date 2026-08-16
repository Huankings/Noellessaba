package org.agmas.noellesroles.modifiers.dual_personality;

import org.agmas.harpymodloader.api.assignment.ModifierAssignmentApi;
import org.agmas.harpymodloader.api.assignment.ModifierAssignmentLifecycleContext;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 双重人格词条的 Harpy 分配接入。
 *
 * <p>这里拆成词条自己的接入类：分配开始时刷新随机生成上限，
 * 分配结束但公告前消费管理员指定的强制主/副人格配对。</p>
 */
public final class DualPersonalityModifierAssignmentRules {
    private static boolean initialized = false;

    private DualPersonalityModifierAssignmentRules() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ModifierAssignmentApi.registerBeforeAssignmentHandler(
                NoellesRolesCore.id("dual_personality_refresh_modifier_limit"),
                0,
                context -> DualPersonalityManager.refreshModifierMaximum(context.serverWorld(), context.players())
        );
        ModifierAssignmentApi.registerBeforeAnnouncementHandler(
                NoellesRolesCore.id("forced_dual_personality_before_modifier_announcement"),
                0,
                DualPersonalityModifierAssignmentRules::applyForcedPairs
        );
    }

    private static void applyForcedPairs(ModifierAssignmentLifecycleContext context) {
        ForcedDualPersonalityManager.ApplyResult result = ForcedDualPersonalityManager.consumeAndApplyPendingPairs(context.serverWorld(), context.players());
        if (result.changedAnything()) {
            NoellesRolesCore.LOGGER.info(
                    "已应用强制双重人格配对：成功 {} 对，因玩家未参与而作废 {} 对。",
                    result.appliedPairs(),
                    result.skippedPairs()
            );
        }
    }
}
