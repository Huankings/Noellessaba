package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.combat.KnifeCooldownApi;
import dev.doctor4t.wathe.api.combat.KnifeCooldownContext;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 杰森成功用匕首处决重伤目标时免除本次冷却。 */
public final class JasonKnifeCooldownHandler {
    private static final int PRIORITY = 300;
    private static boolean initialized;

    private JasonKnifeCooldownHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        KnifeCooldownApi.registerCooldownModifier(
                NoellesRolesCore.id("jason/knife_execution_cooldown"),
                PRIORITY,
                JasonKnifeCooldownHandler::modify
        );
    }

    private static int modify(KnifeCooldownContext context, int currentCooldown) {
        /* 只有 DeathApi 已确认处决成功才会存在这个一次性标记。 */
        return JasonWoundManager.consumeKnifeExecutionNoCooldown(context.attacker()) ? 0 : currentCooldown;
    }
}
