package org.agmas.noellesroles.client.roles.controller;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.controller.ControlledPlayerComponent;

/** 接管操控者“被控制玩家”的按键与滚轮限制。 */
public final class ControllerInputRules {
    private static final int PRIORITY = 5000;

    private ControllerInputRules() {
    }

    public static void register() {
        ClientInputApi.registerKeyBindingRule(NoellesRolesCore.id("input/controller/keys"), PRIORITY, context -> {
            MinecraftClient client = context.client();
            if (!isControlled(client) || !isBlockedKey(client, context.keyBinding())) {
                return ClientInputApi.KeyResult.pass();
            }
            return ClientInputApi.KeyResult.block();
        });

        ClientInputApi.registerMouseScrollRule(NoellesRolesCore.id("input/controller/scroll"), PRIORITY, context ->
                isControlled(context.client()) && context.client().player.isAlive()
                        ? ClientInputApi.Decision.BLOCK
                        : ClientInputApi.Decision.PASS
        );
    }

    private static boolean isControlled(MinecraftClient client) {
        return client.player != null && ControlledPlayerComponent.KEY.get(client.player).isControlled;
    }

    private static boolean isBlockedKey(MinecraftClient client, KeyBinding key) {
        if (client.options == null) {
            return false;
        }
        if (key.equals(client.options.forwardKey)
                || key.equals(client.options.backKey)
                || key.equals(client.options.leftKey)
                || key.equals(client.options.rightKey)
                || key.equals(client.options.attackKey)
                || key.equals(client.options.useKey)
                || key.equals(client.options.chatKey)
                || key.equals(client.options.sneakKey)
                || key.equals(client.options.inventoryKey)) {
            return true;
        }
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (key.equals(hotbarKey)) {
                return true;
            }
        }
        return false;
    }
}
