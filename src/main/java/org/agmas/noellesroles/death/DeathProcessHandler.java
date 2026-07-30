package org.agmas.noellesroles.death;

import dev.doctor4t.wathe.api.death.DeathApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 统一维护“某名玩家正在处理死亡流程”的标记。
 *
 * <p>控制、巫毒等递归死亡机制只需要查询这个组件，
 * 不再各自 mixin 到 {@code GameFunctions.killPlayer} 开头和返回点。</p>
 */
public final class DeathProcessHandler {
    private static boolean initialized = false;

    private DeathProcessHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerBeforeAttempt(
                NoellesRolesCore.id("death_process_start"),
                DeathApi.PRIORITY_DEATH_PROCESS_STATE,
                /*
                 * 这个阶段早于 AllowPlayerDeath 和疯魔护盾。
                 * 即使后面被免死/护盾拦下，也会进入 afterAttempt 清理，
                 * 因此可以稳定表示“当前线程里这名玩家已有一个死亡请求正在展开”。
                 */
                context -> DeathProcessComponent.KEY.get(context.victim()).setProcessing(true)
        );
        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("death_process_cleanup"),
                DeathApi.PRIORITY_FINAL_CLEANUP,
                /*
                 * 用最低优先级最后复位，让其他 afterAttempt 处理器仍能读到 processing 状态。
                 * Timekeeper 早期吞重复死亡的特殊分支不会进入这里，所以它会在自己的 handler 中手动清理。
                 */
                context -> DeathProcessComponent.KEY.get(context.victim()).setProcessing(false)
        );
    }
}
