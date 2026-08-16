package org.agmas.noellesroles.modifiers.lovers;

import org.agmas.harpymodloader.api.assignment.ModifierAssignmentApi;
import org.agmas.harpymodloader.api.assignment.ModifierAssignmentLifecycleContext;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 恋人词条的 Harpy 分配接入。
 *
 * <p>强制恋人指令需要在 Harpy 随机/forceModifier 分配完成之后、公告之前消费。
 * 旧实现 mixin 了 assignModifiers 的内部调用点；现在改为 Harpy 的公告前生命周期回调。</p>
 */
public final class LoversModifierAssignmentRules {
    private static boolean initialized = false;

    private LoversModifierAssignmentRules() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ModifierAssignmentApi.registerBeforeAnnouncementHandler(
                NoellesRolesCore.id("forced_lovers_before_modifier_announcement"),
                0,
                LoversModifierAssignmentRules::applyForcedPairs
        );
    }

    private static void applyForcedPairs(ModifierAssignmentLifecycleContext context) {
        ForcedLoversManager.ApplyResult result = ForcedLoversManager.consumeAndApplyPendingPairs(context.serverWorld(), context.players());
        if (result.changedAnything()) {
            NoellesRolesCore.LOGGER.info(
                    "已应用强制恋人配对：成功 {} 对，因玩家未参与而作废 {} 对。",
                    result.appliedPairs(),
                    result.skippedPairs()
            );
        }
    }
}
