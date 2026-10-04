package org.agmas.noellesroles.roles.perceiver;

import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 感知者完成真实任务后的背包能力冷却缩减。 */
public final class PerceiverTaskCompletionHandler {
    private static boolean initialized;

    private PerceiverTaskCompletionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        /*
         * 使用 Wathe 的任务完成事件而不是监听心情变化，确保调试指令、治疗和其它心情来源
         * 不会被错误计算成“完成一个任务”。通用冷却组件会负责最低钳制到 0 并同步客户端。
         */
        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            if (context.role() == NoellesRoleRegistry.PERCEIVER) {
                AbilityPlayerComponent.KEY.get(context.player())
                        .changeCooldown(-PerceiverConstants.TASK_COOLDOWN_REDUCTION_TICKS);
            }
        });
    }
}
