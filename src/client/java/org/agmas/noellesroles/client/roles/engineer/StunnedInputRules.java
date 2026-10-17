package org.agmas.noellesroles.client.roles.engineer;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.lwjgl.glfw.GLFW;

/** 工程师捕捉装置造成的定身输入规则。 */
public final class StunnedInputRules {
    private static final int PRIORITY = 5000;

    private StunnedInputRules() {
    }

    public static void register() {
        ClientInputApi.registerKeyBindingRule(NoellesRolesCore.id("input/stunned/keys"), PRIORITY, context -> {
            if (!StunnedInputLock.shouldBlockKey(context.client(), context.keyBinding())) {
                return ClientInputApi.KeyResult.pass();
            }
            StunnedInputLock.releaseBlockedKeys(context.client());
            return ClientInputApi.KeyResult.block();
        });

        ClientInputApi.registerKeyboardRule(NoellesRolesCore.id("input/stunned/keyboard"), PRIORITY, context -> {
            if (!StunnedInputLock.shouldBlockKeyboardPress(context.client(), context.key(), context.scanCode())) {
                return ClientInputApi.Decision.PASS;
            }
            StunnedInputLock.releaseBlockedKeys(context.client());
            return context.action() == GLFW.GLFW_PRESS || context.action() == GLFW.GLFW_REPEAT
                    ? ClientInputApi.Decision.BLOCK
                    : ClientInputApi.Decision.ALLOW;
        });

        ClientInputApi.registerMouseButtonRule(NoellesRolesCore.id("input/stunned/mouse_button"), PRIORITY, context -> {
            if (!StunnedInputLock.shouldBlockMouseButton(context.client(), context.button())) {
                return ClientInputApi.Decision.PASS;
            }
            StunnedInputLock.releaseBlockedKeys(context.client());
            return ClientInputApi.Decision.BLOCK;
        });

        ClientInputApi.registerMouseScrollRule(NoellesRolesCore.id("input/stunned/mouse_scroll"), PRIORITY, context -> {
            if (!StunnedInputLock.isInputLocked(context.client())) {
                return ClientInputApi.Decision.PASS;
            }
            StunnedInputLock.releaseBlockedKeys(context.client());
            return ClientInputApi.Decision.BLOCK;
        });

        ClientInputApi.registerInputTickHandler(NoellesRolesCore.id("input/stunned/tick_cleanup"), PRIORITY, context -> {
            if (StunnedInputLock.isInputLocked(context.client())) {
                StunnedInputLock.releaseBlockedKeys(context.client());
            }
        });

        ClientInputApi.registerActionRule(NoellesRolesCore.id("input/stunned/actions"), PRIORITY, context -> {
            if (!StunnedInputLock.isInputLocked(context.client())) {
                return ClientInputApi.ActionResult.pass();
            }
            StunnedInputLock.releaseBlockedKeys(context.client());
            return ClientInputApi.ActionResult.block();
        });
    }
}
