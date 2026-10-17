package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.lwjgl.glfw.GLFW;

/** 杰森重伤倒地与“无恶不在”期间的客户端输入规则。 */
public final class JasonInputRules {
    private static final int PRIORITY = 4900;

    private JasonInputRules() {
    }

    public static void register() {
        ClientInputApi.registerKeyBindingRule(NoellesRolesCore.id("input/jason/keys"), PRIORITY, context -> {
            boolean wounded = JasonWoundedInputLock.shouldBlockKey(context.client(), context.keyBinding());
            boolean ability = JasonAbilityInputLock.shouldBlockKey(context.client(), context.keyBinding());
            if (!wounded && !ability) {
                return ClientInputApi.KeyResult.pass();
            }
            release(context.client(), wounded, ability);
            return ClientInputApi.KeyResult.block();
        });

        ClientInputApi.registerKeyboardRule(NoellesRolesCore.id("input/jason/keyboard"), PRIORITY, context -> {
            boolean wounded = JasonWoundedInputLock.shouldBlockKeyboardPress(context.client(), context.key(), context.scanCode());
            boolean ability = JasonAbilityInputLock.shouldBlockKeyboardPress(context.client(), context.key(), context.scanCode());
            if (!wounded && !ability) {
                return ClientInputApi.Decision.PASS;
            }
            release(context.client(), wounded, ability);
            return context.action() == GLFW.GLFW_PRESS || context.action() == GLFW.GLFW_REPEAT
                    ? ClientInputApi.Decision.BLOCK
                    : ClientInputApi.Decision.ALLOW;
        });

        ClientInputApi.registerMouseButtonRule(NoellesRolesCore.id("input/jason/mouse"), PRIORITY, context -> {
            boolean wounded = JasonWoundedInputLock.shouldBlockMouseButton(context.client(), context.button());
            boolean ability = JasonAbilityInputLock.shouldBlockMouseButton(context.client(), context.button());
            if (!wounded && !ability) {
                return ClientInputApi.Decision.PASS;
            }
            release(context.client(), wounded, ability);
            return ClientInputApi.Decision.BLOCK;
        });

        ClientInputApi.registerInputTickHandler(NoellesRolesCore.id("input/jason/tick_cleanup"), PRIORITY, context -> {
            boolean wounded = JasonWoundedInputLock.isInputLocked(context.client());
            boolean ability = JasonAbilityInputLock.isInputLocked(context.client());
            release(context.client(), wounded, ability);
        });

        ClientInputApi.registerActionRule(NoellesRolesCore.id("input/jason/actions"), PRIORITY, context -> {
            boolean wounded = JasonWoundedInputLock.isInputLocked(context.client());
            boolean ability = JasonAbilityInputLock.isInputLocked(context.client());
            if (!wounded && !ability) {
                return ClientInputApi.ActionResult.pass();
            }
            release(context.client(), wounded, ability);
            return ClientInputApi.ActionResult.block();
        });
    }

    private static void release(net.minecraft.client.MinecraftClient client, boolean wounded, boolean ability) {
        if (wounded) {
            JasonWoundedInputLock.releaseBlockedKeys(client);
        }
        if (ability) {
            JasonAbilityInputLock.releaseBlockedKeys(client);
        }
    }
}
