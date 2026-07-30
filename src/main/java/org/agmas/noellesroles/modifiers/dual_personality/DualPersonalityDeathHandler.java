package org.agmas.noellesroles.modifiers.dual_personality;

import dev.doctor4t.wathe.api.death.DeathApi;
import dev.doctor4t.wathe.api.death.DeathDecision;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 双重人格词条的死亡拦截和确认击杀奖励。
 */
public final class DualPersonalityDeathHandler {
    private static boolean initialized = false;

    private DualPersonalityDeathHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        DeathApi.registerEarlyInterceptor(
                NoellesRolesCore.id("dual_personality_dormant_death_guard"),
                DeathApi.PRIORITY_SPECIAL_SURVIVAL_PROTECTION,
                context -> {
                    ServerPlayerEntity victim = context.serverVictim();
                    /*
                     * 休眠人格理论上不该承受常规死亡。
                     * 这里放在 early 阶段，是为了在 Wathe 写“死亡处理中”标记前就吞掉无效死亡请求，
                     * 避免后续流程误记录回放、结算奖励或生成尸体。
                     */
                    return victim != null && DualPersonalityManager.tryProtectDormantFatalDeath(victim)
                            ? DeathDecision.CANCEL
                            : DeathDecision.PASS;
                }
        );

        DeathApi.registerFatalInterceptor(
                NoellesRolesCore.id("dual_personality_fatal_intercept"),
                DeathApi.PRIORITY_FATAL_INTERCEPT,
                context -> {
                    ServerPlayerEntity victim = context.serverVictim();
                    /*
                     * 活跃人格被真正打到致死时，双重人格可能把死亡改写成“双活解离”等状态变化。
                     * 这个判定必须放在 AllowPlayerDeath/护盾之后、切旁观之前，
                     * 否则会抢在普通免死前执行，或来不及阻止 Wathe 生成尸体。
                     */
                    return victim != null && DualPersonalityManager.tryInterceptFatalDeath(victim)
                            ? DeathDecision.CANCEL
                            : DeathDecision.PASS;
                }
        );

        DeathApi.registerAfterAttempt(
                NoellesRolesCore.id("dual_personality_after_death"),
                DeathApi.PRIORITY_POST_CONFIRMED_DEATH,
                context -> {
                    if (context.killer() instanceof ServerPlayerEntity killer
                            && GameConstants.DeathReasons.KNIFE.equals(context.deathReason())) {
                        /*
                         * 刀击确认杀人后通知双重人格管理器。
                         * 这里不要求 victim 一定是双重人格，因为 manager 内部负责判断击杀者/目标状态。
                         */
                        DualPersonalityManager.onSuccessfulKill(killer, context.victim(), context.deathReason());
                    }
                    ServerPlayerEntity victim = context.serverVictim();
                    if (victim != null) {
                        // 死亡流程结束后恢复休眠人格语音频道，避免死亡中的临时隔离状态残留。
                        DualPersonalityManager.restoreDormantVoiceChannelAfterDeath(victim);
                    }
                }
        );
    }
}
