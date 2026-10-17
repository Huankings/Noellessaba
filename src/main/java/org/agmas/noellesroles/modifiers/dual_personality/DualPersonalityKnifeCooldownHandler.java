package org.agmas.noellesroles.modifiers.dual_personality;

import dev.doctor4t.wathe.api.combat.KnifeCooldownApi;
import dev.doctor4t.wathe.api.combat.KnifeCooldownContext;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 双活阶段将匕首冷却缩短到 1 秒的规则。 */
public final class DualPersonalityKnifeCooldownHandler {
    private static final int PRIORITY = 100;
    private static boolean initialized;

    private DualPersonalityKnifeCooldownHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        KnifeCooldownApi.registerCooldownModifier(
                NoellesRolesCore.id("dual_personality/knife_cooldown"),
                PRIORITY,
                DualPersonalityKnifeCooldownHandler::modify
        );
    }

    private static int modify(KnifeCooldownContext context, int currentCooldown) {
        if (!DualPersonalityManager.isActiveRound(context.attacker().getWorld())
                || !DualPersonalityComponent.KEY.get(context.attacker().getWorld()).isDoubleActive(context.attacker().getUuid())) {
            return currentCooldown;
        }
        /* 只缩短不拉长，避免把更高优先级的“无冷却”结果重新改回 1 秒。 */
        return Math.min(currentCooldown, DualPersonalityConstants.DOUBLE_ACTIVE_KNIFE_COOLDOWN_TICKS);
    }
}
