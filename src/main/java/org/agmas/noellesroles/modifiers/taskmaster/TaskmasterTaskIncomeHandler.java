package org.agmas.noellesroles.modifiers.taskmaster;

import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.economy.EconomyApi;
import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.util.Identifier;
import org.agmas.harpymodloader.component.WorldModifierComponent;

public final class TaskmasterTaskIncomeHandler {
    private static boolean initialized = false;

    private TaskmasterTaskIncomeHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TaskCompletionApi.registerTaskIncomeProvider(
                Identifier.of(NoellesRolesCore.MOD_ID, "taskmaster_task_income"),
                TaskCompletionApi.DEFAULT_PRIORITY,
                context -> {
                    /*
                     * Wathe 的旧任务金币 provider 只会给非杀手语义玩家结算；
                     * 杀手分支会在下面的 AFTER_TASK_COMPLETE 中单独补发，避免同一项任务重复加钱。
                     */
                    if (context.gameWorld().canUseKillerFeatures(context.player())) {
                        return 0;
                    }
                    return getTaskmasterTaskIncome(context);
                }
        );

        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            if (!context.gameWorld().canUseKillerFeatures(context.player())) {
                return;
            }

            int income = getTaskmasterTaskIncome(context);
            if (income > 0) {
                /*
                 * 杀手任务现在会跳过旧任务金币 provider，所以任务大师必须在真实任务完成事件里补发金币。
                 * 这里仍使用 Wathe 的商店余额组件入口，保证服务端余额和客户端金币 HUD 正常同步。
                 */
                PlayerShopComponent.KEY.get(context.player()).addToBalance(income);
            }
        });
    }

    public static boolean canApplyToTaskIncomeRole(TaskCompletionApi.TaskCompletionContext context) {
        return hasTaskmaster(context) && EconomyApi.shouldRenderBalanceHud(context.gameWorld(), context.player());
    }

    private static int getTaskmasterTaskIncome(TaskCompletionApi.TaskCompletionContext context) {
        if (!canApplyToTaskIncomeRole(context)) {
            return 0;
        }

        /*
         * 沿用 kinssaba 原语义：任务大师在杀手语义职业上每个任务 50 金币，
         * 在非杀手但显示金币 HUD 的职业上每个任务额外 25 金币。
         */
        Role role = context.role();
        return role != null && role.canUseKiller()
                ? TaskmasterConstants.KILLER_TASK_INCOME
                : TaskmasterConstants.NON_KILLER_TASK_INCOME;
    }

    private static boolean hasTaskmaster(TaskCompletionApi.TaskCompletionContext context) {
        WorldModifierComponent modifier = WorldModifierComponent.KEY.get(context.player().getWorld());
        return modifier.isModifier(context.player(), NoellesModifierRegistry.TASKMASTER);
    }
}
