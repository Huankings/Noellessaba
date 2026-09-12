package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.task.MoodTaskApi;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 亡命徒存活期间阻止 Wathe 默认和外部心情任务发放。 */
public final class OutlawMoodTaskHandler {
    private static boolean initialized;

    private OutlawMoodTaskHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        MoodTaskApi.registerAssignmentRule(NoellesRolesCore.id("outlaw_no_mood_tasks"), 200, context ->
                context.role() == NoellesRoleRegistry.OUTLAW
                        ? MoodTaskApi.AssignmentDecision.DENY
                        : MoodTaskApi.AssignmentDecision.PASS
        );
    }
}
