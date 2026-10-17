package org.agmas.noellesroles.client.roles.spiritualist;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.spiritualist.SpiritualistHostComponent;

/** 灵术师附身、出窍和特殊相机的客户端输入规则。 */
public final class SpiritualistInputRules {
    private static final int PRIORITY = 4995;

    private SpiritualistInputRules() {
    }

    public static void register() {
        ClientInputApi.registerKeyBindingRule(NoellesRolesCore.id("input/spiritualist/keys"), PRIORITY, context -> {
            MinecraftClient client = context.client();
            KeyBinding key = context.keyBinding();

            /* 出窍相机不能借由数字键切换本体热键栏。 */
            if (SpiritualistClientController.isProjectionActive() && isHotbarKey(client, key)) {
                return ClientInputApi.KeyResult.block();
            }

            if (client.player == null || !SpiritualistHostComponent.KEY.get(client.player).possessed) {
                return ClientInputApi.KeyResult.pass();
            }
            return isPossessedBlockedKey(client, key)
                    ? ClientInputApi.KeyResult.block()
                    : ClientInputApi.KeyResult.pass();
        });

        ClientInputApi.registerMouseScrollRule(NoellesRolesCore.id("input/spiritualist/scroll"), PRIORITY, context ->
                context.client().player != null
                        && SpiritualistHostComponent.KEY.get(context.client().player).possessed
                        ? ClientInputApi.Decision.BLOCK
                        : ClientInputApi.Decision.PASS
        );

        ClientInputApi.registerActionRule(NoellesRolesCore.id("input/spiritualist/actions"), PRIORITY, context -> {
            boolean projection = SpiritualistClientController.isProjectionActive();
            boolean possessionView = SpiritualistClientController.isPossessionViewActive();
            return switch (context.action()) {
                case ATTACK, BLOCK_BREAKING -> projection || possessionView
                        ? ClientInputApi.ActionResult.block() : ClientInputApi.ActionResult.pass();
                case ITEM_PICK -> projection ? ClientInputApi.ActionResult.block() : ClientInputApi.ActionResult.pass();
                case ITEM_USE -> {
                    if (possessionView) {
                        /* 附身视角右键仍要交给灵术师自己的即时交互转发，然后阻止原版本体使用物品。 */
                        SpiritualistClientController.handleImmediatePossessionUseAttempt();
                        yield ClientInputApi.ActionResult.block();
                    }
                    yield projection ? ClientInputApi.ActionResult.block() : ClientInputApi.ActionResult.pass();
                }
            };
        });
    }

    private static boolean isPossessedBlockedKey(MinecraftClient client, KeyBinding key) {
        if (client.options == null) {
            return false;
        }
        return key.equals(client.options.forwardKey)
                || key.equals(client.options.backKey)
                || key.equals(client.options.leftKey)
                || key.equals(client.options.rightKey)
                || key.equals(client.options.jumpKey)
                || key.equals(client.options.sprintKey)
                || key.equals(client.options.attackKey)
                || key.equals(client.options.useKey)
                || key.equals(client.options.sneakKey)
                || key.equals(client.options.inventoryKey)
                || isHotbarKey(client, key);
    }

    private static boolean isHotbarKey(MinecraftClient client, KeyBinding key) {
        if (client.options == null) {
            return false;
        }
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (key.equals(hotbarKey)) {
                return true;
            }
        }
        return false;
    }
}
