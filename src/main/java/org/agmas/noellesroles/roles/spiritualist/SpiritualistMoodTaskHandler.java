package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.api.task.MoodTaskApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 灵术师与 Wathe 心情任务完成流程的接入。
 *
 * <p>旧版通过 mixin {@code PlayerMoodComponent#completeTask} 阻止附身期间完成任务。
 * 现在 Wathe 已经开放任务完成拦截 API，所以这里改为注册规则：</p>
 * <p>1. 灵术师自己正在附身时，不能借宿主身体偷偷完成自己的任务；</p>
 * <p>2. 被附身者处于失控状态时，也不能被系统判定为完成自己的任务。</p>
 */
public final class SpiritualistMoodTaskHandler {
    private static boolean initialized = false;

    private SpiritualistMoodTaskHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        MoodTaskApi.registerCompletionRule(
                NoellesRolesCore.id("spiritualist/block_possession_task_completion"),
                MoodTaskApi.DEFAULT_PRIORITY + 100,
                context -> SpiritualistPlayerComponent.KEY.get(context.player()).isPossessing()
                        || SpiritualistHostComponent.KEY.get(context.player()).possessed
                        ? MoodTaskApi.CompletionDecision.DENY
                        : MoodTaskApi.CompletionDecision.PASS
        );
    }
}
