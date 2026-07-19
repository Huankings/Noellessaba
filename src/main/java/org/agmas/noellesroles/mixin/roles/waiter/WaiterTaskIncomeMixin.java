package org.agmas.noellesroles.mixin.roles.waiter;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.roles.waiter.WaiterInteractionHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务员递送完成任务时的金币收入修正。
 *
 * <p>Wathe 的 TaskCompletionApi 会在任务完成时自动给完成任务的玩家发金币。
 * 但服务员“帮别人完成一次任务”时，需求要求目标不能因为这次交互额外拿任务收入金币，
 * 而是由服务员拿 50 + 25。因此 WaiterInteractionHandler 会先登记一个临时 suppress 标记，
 * 这里在 Wathe 发钱前拦截并跳过收入计算，只保留 AFTER_TASK_COMPLETE 事件。</p>
 */
@Mixin(TaskCompletionApi.class)
public abstract class WaiterTaskIncomeMixin {
    @Shadow @Final public static Event<TaskCompletionApi.AfterTaskComplete> AFTER_TASK_COMPLETE;

    @Inject(method = "handleTaskCompleted", at = @At("HEAD"), cancellable = true)
    private static void noellesroles$suppressServedTaskIncome(
            ServerPlayerEntity player,
            GameWorldComponent gameWorld,
            PlayerMoodComponent.Task task,
            boolean rewardedMood,
            CallbackInfo ci
    ) {
        // 没有服务员登记过 suppress 的任务，完全走 Wathe 原流程。
        if (!WaiterInteractionHandler.shouldSuppressServedTaskIncome(player, task)) {
            return;
        }

        /*
         * 虽然取消了默认收入流程，但 AFTER_TASK_COMPLETE 仍要触发。
         * 服务员被动透视、其他扩展监听任务完成等逻辑都依赖这个事件。
         */
        Role role = gameWorld.getRole(player);
        TaskCompletionApi.TaskCompletionContext context =
                new TaskCompletionApi.TaskCompletionContext(player, gameWorld, role, task, rewardedMood);
        AFTER_TASK_COMPLETE.invoker().afterTaskComplete(context);
        ci.cancel();
    }
}
