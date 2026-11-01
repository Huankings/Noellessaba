package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 小偷完成真实任务后减少悦灵背包能力冷却。 */
public final class ThiefTaskCompletionHandler {
    private static boolean initialized;

    private ThiefTaskCompletionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            if (context.role() == NoellesRoleRegistry.THIEF) {
                ThiefPlayerComponent.KEY.get(context.player())
                        .reduceSummonCooldown(ThiefConstants.ALLAY_TASK_COOLDOWN_REDUCTION_TICKS);
            }
        });
    }
}
