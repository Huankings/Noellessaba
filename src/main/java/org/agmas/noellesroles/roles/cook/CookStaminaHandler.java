package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.api.stamina.PlayerStaminaApi;
import dev.doctor4t.wathe.api.stamina.PlayerStaminaApi.StaminaConsumptionResult;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 厨师疯魔的体力消耗规则，由移动 bootstrap 统一注册。
 *
 * <p>只依赖 Wathe 已同步的疯魔 profile 与存活状态，通用初始化注册后两端都能判断。
 * 规则本身不保存“之前的体力上限”或独立倒计时；疯魔自然结束、强制结束、死亡、停局和回溯
 * 直接以当前 profile 为准，避免另一套状态和 Wathe 疯魔倒计时失步。</p>
 */
public final class CookStaminaHandler {
    private CookStaminaHandler() {
    }

    public static void init() {
        PlayerStaminaApi.registerConsumptionRule(
                NoellesRolesCore.id("stamina/cook_psycho"),
                PlayerStaminaApi.DEFAULT_PRIORITY,
                context -> {
                    // 无职业、未开局和普通死亡玩家不应用厨师规则；调试疯魔也沿用同一个 profile 判断。
                    if (!context.gameWorld().isRunning()
                            || context.role() == null
                            || !GameFunctions.isPlayerAliveAndSurvival(context.player())
                            || !CookPsychoHandler.isCookPsychoActive(context.player())) {
                        return StaminaConsumptionResult.PASS;
                    }
                    // 跳过正常消耗；强制 drain/clear/set 仍然生效，体力上限与低心情禁跑规则也继续保留。
                    return StaminaConsumptionResult.SKIP;
                }
        );
    }
}
