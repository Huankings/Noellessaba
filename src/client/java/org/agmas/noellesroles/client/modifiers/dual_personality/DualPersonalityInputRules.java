package org.agmas.noellesroles.client.modifiers.dual_personality;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.lwjgl.glfw.GLFW;

/** 双重人格休眠旁观相机的脱离键和旁观目标键限制。 */
public final class DualPersonalityInputRules {
    private static final int PRIORITY = 5000;

    private DualPersonalityInputRules() {
    }

    public static void register() {
        ClientInputApi.registerKeyboardRule(NoellesRolesCore.id("input/dual_personality/dormant"), PRIORITY, context -> {
            if (!DualPersonalityClientState.isDormant(context.client().player)
                    || !DualPersonalityClientState.isDormantBlockedKey(context.client(), context.key(), context.scanCode())) {
                return ClientInputApi.Decision.PASS;
            }
            DualPersonalityClientState.releaseDormantBlockedKeys(context.client());
            return context.action() == GLFW.GLFW_PRESS || context.action() == GLFW.GLFW_REPEAT
                    ? ClientInputApi.Decision.BLOCK
                    : ClientInputApi.Decision.ALLOW;
        });
    }
}
