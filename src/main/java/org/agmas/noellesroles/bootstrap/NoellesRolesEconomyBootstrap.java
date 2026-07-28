package org.agmas.noellesroles.bootstrap;

import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.economy.EconomyApi;
import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.modifiers.magnate.MagnateEconomyHandler;
import org.agmas.noellesroles.modifiers.taskmaster.TaskmasterTaskIncomeHandler;
import org.agmas.noellesroles.roles.initiate.InitiateConstants;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * NoellesRoles 的经济系统注册。
 *
 * <p>这里集中处理金币 HUD、任务收入和被动收入规则。
 * 这样后续想调整职业经济，不必再翻入口类里那一长段逻辑。</p>
 */
public final class NoellesRolesEconomyBootstrap {
    private NoellesRolesEconomyBootstrap() {
    }

    public static void init() {
        EconomyApi.registerCurrency(
                TimekeeperConstants.TIME_CURRENCY_ID,
                TimekeeperConstants.TIME_CURRENCY_ICON,
                "currency.noellesroles.time",
                context -> context.role() == NoellesRoleRegistry.TIMEKEEPER
        );

        EconomyApi.registerBalanceHudRoles(List.of(
                NoellesRoleRegistry.BARTENDER,
                NoellesRoleRegistry.BELLRINGER,
                NoellesRoleRegistry.DETECTIVE,
                NoellesRoleRegistry.RECALLER,
                NoellesRoleRegistry.EXECUTIONER,
                NoellesRoleRegistry.JESTER,
                NoellesRoleRegistry.NOISEMAKER,
                NoellesRoleRegistry.WINDER,
                NoellesRoleRegistry.MIMIC,
                NoellesRoleRegistry.TRAPPER,
                NoellesRoleRegistry.CORONER,
                NoellesRoleRegistry.PROPHET,
                NoellesRoleRegistry.REMEMBERER,
                NoellesRoleRegistry.ENGINEER,
                NoellesRoleRegistry.DREAMER,
                NoellesRoleRegistry.HACKER,
                NoellesRoleRegistry.COWARD,
                NoellesRoleRegistry.WAITER,
                NoellesRoleRegistry.COOK,
                NoellesRoleRegistry.PHYSICIAN,
                NoellesRoleRegistry.INITIATE,
                NoellesRoleRegistry.LICENSED_VILLAIN,
                NoellesRoleRegistry.TIMEKEEPER
        ));

        /*
         * 任务金币名单要允许去重，否则重复补职业时容易在模组启动阶段直接炸掉。
         * 这里保留 LinkedHashSet 是为了维持旧顺序，同时自动抹平重复条目。
         */
        Set<Role> taskIncomeRoles = new LinkedHashSet<>(List.of(
                NoellesRoleRegistry.PHANTOM,
                NoellesRoleRegistry.SWAPPER,
                NoellesRoleRegistry.TRAPPER,
                NoellesRoleRegistry.RECALLER,
                NoellesRoleRegistry.BARTENDER,
                NoellesRoleRegistry.BELLRINGER,
                NoellesRoleRegistry.DETECTIVE,
                NoellesRoleRegistry.MORPHLING,
                NoellesRoleRegistry.NOISEMAKER,
                NoellesRoleRegistry.CORPSEMAKER,
                NoellesRoleRegistry.CONTROLLER,
                NoellesRoleRegistry.CORONER,
                NoellesRoleRegistry.ENGINEER,
                NoellesRoleRegistry.ROBBER,
                NoellesRoleRegistry.CLEANER,
                NoellesRoleRegistry.HUNTER,
                NoellesRoleRegistry.BOMBER,
                NoellesRoleRegistry.STALKER,
                NoellesRoleRegistry.BRAINWASHER,
                NoellesRoleRegistry.WINDER,
                NoellesRoleRegistry.PROPHET,
                NoellesRoleRegistry.HACKER,
                NoellesRoleRegistry.COWARD,
                NoellesRoleRegistry.REMEMBERER,
                NoellesRoleRegistry.WAITER,
                NoellesRoleRegistry.COOK,
                NoellesRoleRegistry.PHYSICIAN,
                NoellesRoleRegistry.MAGICIAN,
                NoellesRoleRegistry.ASSASSIN,
                NoellesRoleRegistry.TIMEKEEPER,
                NoellesRoleRegistry.LICENSED_VILLAIN,
                NoellesRoleRegistry.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES
        ));
        TaskCompletionApi.registerTaskIncomeProvider(
                org.agmas.noellesroles.registry.NoellesRolesCore.id("task_income"),
                TaskCompletionApi.DEFAULT_PRIORITY,
                context -> taskIncomeRoles.contains(context.role())
                        ? (context.role() == NoellesRoleRegistry.LICENSED_VILLAIN ? LicensedVillainConstants.TASK_INCOME_COINS : 50)
                        : 0
        );
        /*
         * 初学者从 StupidExpress 迁移后继续按每个任务 50 金币发放；
         * 该数值单独走 InitiateConstants，后续调职业时不会混进其它 Noelles 通用任务收入。
         */
        TaskCompletionApi.registerTaskIncomeProvider(
                NoellesRolesCore.id("initiate_task_income"),
                TaskCompletionApi.DEFAULT_PRIORITY,
                context -> context.role() == NoellesRoleRegistry.INITIATE ? InitiateConstants.TASK_INCOME_COINS : 0
        );

        /*
         * 扒手的金币来源来自专属逻辑，不吃 Wathe 默认的通用被动收入。
         */
        EconomyApi.registerPassiveIncomeRule(
                org.agmas.noellesroles.registry.NoellesRolesCore.id("avaricious_no_default_passive_income"),
                EconomyApi.DEFAULT_PRIORITY,
                context -> context.role() == NoellesRoleRegistry.AVARICIOUS
                        ? EconomyApi.PassiveIncomeDecision.DENY
                        : EconomyApi.PassiveIncomeDecision.PASS
        );

        EconomyApi.registerPassiveIncomeRoles(List.of(
                NoellesRoleRegistry.NOISEMAKER,
                NoellesRoleRegistry.MIMIC,
                NoellesRoleRegistry.JESTER,
                NoellesRoleRegistry.EXECUTIONER,
                NoellesRoleRegistry.DREAMER,
                NoellesRoleRegistry.HACKER,
                NoellesRoleRegistry.CORONER,
                NoellesRoleRegistry.ENGINEER,
                NoellesRoleRegistry.RECALLER,
                NoellesRoleRegistry.COOK,
                NoellesRoleRegistry.TIMEKEEPER
        ));

        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context -> {
            if (context.role() == NoellesRoleRegistry.TIMEKEEPER) {
                PlayerShopComponent.KEY.get(context.player()).addCurrencyAmount(
                        TimekeeperConstants.TIME_CURRENCY_ID,
                        TimekeeperConstants.TASK_TIME_INCOME_AMOUNT
                );
            }
        });

        MagnateEconomyHandler.init();
        TaskmasterTaskIncomeHandler.init();
    }
}
