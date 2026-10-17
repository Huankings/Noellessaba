package org.agmas.noellesroles.client.roles.kidnapper;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;

/** 绑匪迷药控制期间的攻击、使用和选槽限制。 */
public final class KidnapperInputRules {
    private static final int PRIORITY = 5000;

    private KidnapperInputRules() {
    }

    public static void register() {
        ClientInputApi.registerKeyBindingRule(NoellesRolesCore.id("input/kidnapper/keys"), PRIORITY, context -> {
            MinecraftClient client = context.client();
            if (!isControlled(client)) {
                return ClientInputApi.KeyResult.pass();
            }
            KeyBinding key = context.keyBinding();
            return key.equals(client.options.attackKey)
                    || key.equals(client.options.useKey)
                    || isHotbarKey(client, key)
                    ? ClientInputApi.KeyResult.block()
                    : ClientInputApi.KeyResult.pass();
        });

        ClientInputApi.registerMouseScrollRule(NoellesRolesCore.id("input/kidnapper/scroll"), PRIORITY, context ->
                isControlled(context.client()) ? ClientInputApi.Decision.BLOCK : ClientInputApi.Decision.PASS
        );
    }

    private static boolean isControlled(MinecraftClient client) {
        return client.player != null
                && WatheClient.isPlayerAliveAndInSurvival()
                && KidnapperComponent.KEY.get(client.player).controlTicks > 0;
    }

    private static boolean isHotbarKey(MinecraftClient client, KeyBinding key) {
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (key.equals(hotbarKey)) {
                return true;
            }
        }
        return false;
    }
}
