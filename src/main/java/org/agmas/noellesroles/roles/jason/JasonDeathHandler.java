package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.death.DeathApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 杰森倒地状态与 Wathe 死亡流程的衔接。
 */
public final class JasonDeathHandler {
    private JasonDeathHandler() {
    }

    public static void init() {
        /*
         * 先拦“存活玩家试图杀死幽魂杰森”的真正致死结果。
         * 这里放在 fatal interceptor，是因为这一步发生在确认死亡前，能直接把这次击杀改写为无效，
         * 比 afterAttempt 更早，也更符合“无恶不在状态下不受存活玩家伤害”的语义。
         */
        DeathApi.registerFatalInterceptor(
                NoellesRolesCore.id("jason/ability_survival_damage_block"),
                DeathApi.PRIORITY_FATAL_INTERCEPT + 50,
                JasonAbilityManager::protectFromSurvivalFatalDamage
        );

        /*
         * 使用 afterAttempt 是因为杰森需要知道本次死亡是否最终成立：
         * 1. 匕首真的处决倒地玩家后，才清除匕首冷却；
         * 2. 护盾、双重人格或其它免死改写后，也要解除倒地，避免玩家被卡在重伤状态；
         * 3. 这个阶段晚于大部分保护链，又早于最终清理状态复位，能读到完整 DeathContext。
         */
        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("jason/wounded_cleanup"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH + 50,
                JasonWoundManager::handleDeathAttempt
        );
        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("jason/ability_cleanup"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH + 40,
                JasonAbilityManager::handleDeathAttempt
        );
    }
}
