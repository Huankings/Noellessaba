package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.task.MoodTaskApi;
import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.List;

/**
 * 影子小丑的心情任务接入。
 *
 * <p>第一阶段的任务并不是 Wathe 自己的常规心情任务节奏，而是职业专属进度：
 * 每名影子小丑独立完成 4 个任务、独立进入第二阶段，因此完成计数保存在世界组件的玩家分栏里。</p>
 */
public final class ShadowJesterTaskHandler {
    private static boolean initialized = false;

    private ShadowJesterTaskHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        MoodTaskApi.registerAssignmentRule(
                NoellesRolesCore.id("shadow_jester_managed_task_assignment"),
                MoodTaskApi.DEFAULT_PRIORITY + 100,
                context -> {
                    ServerPlayerEntity player = context.player();
                    if (!context.gameWorld().isRole(player, NoellesRoleRegistry.SHADOW_JESTER)) {
                        return MoodTaskApi.AssignmentDecision.PASS;
                    }

                    /*
                     * 影子小丑整局都要拦住 Wathe 的“原生自动任务节奏”：
                     * 1. Wathe 假心情的“空任务栏冷却刷任务”不能进入任务栏；
                     * 2. 低心情补槽或任务完成后的原生补槽也不能抢影子小丑自己的 35~70 秒节奏。
                     *
                     * 但调试指令和其它显式 API 发放属于“管理员/扩展主动行为”，这里必须放行。
                     * 这样 /wathe:moodTask assign 可以直接给影子小丑塞任务做测试，而 Wathe 正常
                     * 自动发放仍会在真正创建任务前被拒绝，客户端也不会再看到任务闪现。
                     */
                    boolean watheInternalAssignment = context.source() == MoodTaskApi.AssignmentSource.INTERNAL_PRIMARY_COOLDOWN
                            || context.source() == MoodTaskApi.AssignmentSource.INTERNAL_SLOT_REFILL;
                    if (watheInternalAssignment) {
                        return MoodTaskApi.AssignmentDecision.DENY;
                    }
                    return MoodTaskApi.AssignmentDecision.PASS;
                }
        );

        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            ServerPlayerEntity player = context.player();
            if (!context.gameWorld().isRole(player, NoellesRoleRegistry.SHADOW_JESTER)) {
                return;
            }

            ShadowJesterComponent component = ShadowJesterComponent.KEY.get(player.getServerWorld());
            if (!component.contains(player.getUuid()) || component.getPhase(player.getUuid()) != ShadowJesterPhase.TASKS) {
                return;
            }

            int completed = component.incrementCompletedTasks(player.getUuid());
            if (completed >= ShadowJesterConstants.REQUIRED_COMPLETED_TASKS) {
                /*
                 * 做满 4 个后立即清掉剩余任务，避免玩家进入第二阶段后任务 HUD 继续占用目标。
                 * 这条清理也会在第一/第二阶段同伴死亡转狂信者时复用。
                 */
                clearAllTasks(player);
                ShadowJesterManager.enterPhaseTwo(player);
                return;
            }

            rememberCurrentManagedTasks(player, component);
            ensureRefillScheduled(player, component);
        });
    }

    public static void prepareInitialTasks(ServerPlayerEntity player) {
        clearAllTasks(player);
        MoodTaskApi.assignRandomTasks(player, ShadowJesterConstants.INITIAL_TASK_COUNT);
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(player.getServerWorld());
        rememberCurrentManagedTasks(player, component);
        component.setRefillTicks(player.getUuid(), -1);
    }

    public static void clearAllTasks(ServerPlayerEntity player) {
        List<Identifier> activeTasks = PlayerMoodComponent.KEY.get(player).getActiveMoodTaskIds();
        for (Identifier taskId : activeTasks) {
            MoodTaskApi.removeTask(player, taskId);
        }
        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(player.getServerWorld());
        component.clearManagedTaskIds(player.getUuid());
        component.setRefillTicks(player.getUuid(), -1);
    }

    public static void tickTaskRefill(ServerPlayerEntity player, ShadowJesterComponent component) {
        if (!component.contains(player.getUuid())) {
            return;
        }
        if (component.getPhase(player.getUuid()) != ShadowJesterPhase.TASKS) {
            clearTaskBookkeepingOutsideTaskPhase(player, component);
            return;
        }
        if (component.getCompletedTasks(player.getUuid()) >= ShadowJesterConstants.REQUIRED_COMPLETED_TASKS) {
            clearTaskBookkeepingOutsideTaskPhase(player, component);
            return;
        }

        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        if (mood.getActiveMoodTaskCount() >= ShadowJesterConstants.INITIAL_TASK_COUNT) {
            component.setRefillTicks(player.getUuid(), -1);
            return;
        }

        int ticks = component.getRefillTicks(player.getUuid());
        if (ticks < 0) {
            ensureRefillScheduled(player, component);
            return;
        }
        if (ticks > 0) {
            component.decrementRefillTicks(player.getUuid());
            return;
        }

        MoodTaskApi.assignRandomTask(player);
        rememberCurrentManagedTasks(player, component);
        /*
         * 只补一个任务，然后重新判断是否还有空槽。
         * 这样“每 35~70 秒补一个，直到再满槽”的节奏不会因为多个空槽一次性全部灌满。
         */
        if (mood.getActiveMoodTaskCount() < ShadowJesterConstants.INITIAL_TASK_COUNT) {
            scheduleNewRefillDelay(player, component);
        } else {
            component.setRefillTicks(player.getUuid(), -1);
        }
    }

    private static void ensureRefillScheduled(ServerPlayerEntity player, ShadowJesterComponent component) {
        if (PlayerMoodComponent.KEY.get(player).getActiveMoodTaskCount() >= ShadowJesterConstants.INITIAL_TASK_COUNT) {
            component.setRefillTicks(player.getUuid(), -1);
            return;
        }
        if (component.getRefillTicks(player.getUuid()) >= 0) {
            /*
             * 已经有一个影子小丑专属补发倒计时在跑时，后续完成其它任务不能重新随机时间。
             * 旧逻辑每完成一个任务都会重排 35~70 秒，导致稳定做任务时补发时间被不断往后推。
             */
            return;
        }

        scheduleNewRefillDelay(player, component);
    }

    private static void scheduleNewRefillDelay(ServerPlayerEntity player, ShadowJesterComponent component) {
        int min = ShadowJesterConstants.TASK_REFILL_MIN_TICKS;
        int max = ShadowJesterConstants.TASK_REFILL_MAX_TICKS;
        int delay = min + player.getRandom().nextInt(Math.max(1, max - min + 1));
        component.setRefillTicks(player.getUuid(), delay);
    }

    private static void rememberCurrentManagedTasks(ServerPlayerEntity player, ShadowJesterComponent component) {
        component.setManagedTaskIds(player.getUuid(), PlayerMoodComponent.KEY.get(player).getActiveMoodTaskIds());
    }

    private static void clearTaskBookkeepingOutsideTaskPhase(ServerPlayerEntity player, ShadowJesterComponent component) {
        if (component.hasManagedTaskIds(player.getUuid())) {
            component.clearManagedTaskIds(player.getUuid());
        }
        if (component.getRefillTicks(player.getUuid()) != -1) {
            component.setRefillTicks(player.getUuid(), -1);
        }
    }
}
