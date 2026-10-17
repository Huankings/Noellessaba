package org.agmas.noellesroles.client.roles.timekeeper;

import dev.doctor4t.wathe.api.client.input.ClientInputApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;
import org.lwjgl.glfw.GLFW;

/** 时停者回溯播放与时间狭缝的输入规则。 */
public final class TimekeeperInputRules {
    private static final int PRIORITY = 5000;

    private TimekeeperInputRules() {
    }

    public static void register() {
        ClientInputApi.registerKeyBindingRule(NoellesRolesCore.id("input/timekeeper/keys"), PRIORITY, context -> {
            MinecraftClient client = context.client();
            KeyBinding key = context.keyBinding();

            if (TimekeeperRewindInputLock.shouldBlockKey(client, key)) {
                TimekeeperRewindInputLock.releaseBlockedKeys(client);
                return ClientInputApi.KeyResult.block();
            }
            if (TimekeeperRiftInputLock.shouldBlockKey(client, key)) {
                TimekeeperRiftInputLock.releaseBlockedKeys(client);
                return ClientInputApi.KeyResult.block();
            }

            /*
             * 狭缝玩家属于 Wathe 特殊存活旁观。地图禁跳时只为该状态恢复真实物理跳跃，
             * 不修改全局 allowJump，也不放开普通活人。
             */
            if (context.query() == ClientInputApi.KeyQuery.IS_PRESSED
                    && shouldRestoreRiftJump(client, key)) {
                return ClientInputApi.KeyResult.override(ClientInputApi.isPhysicallyPressed(client, key));
            }
            return ClientInputApi.KeyResult.pass();
        });

        ClientInputApi.registerKeyboardRule(NoellesRolesCore.id("input/timekeeper/rift_hotbar"), PRIORITY, context -> {
            MinecraftClient client = context.client();
            if (!isInRift(client) || !matchesHotbarKey(client, context.key(), context.scanCode())) {
                return ClientInputApi.Decision.PASS;
            }
            for (KeyBinding hotbarKey : client.options.hotbarKeys) {
                ClientInputApi.releaseKey(hotbarKey);
            }
            return context.action() == GLFW.GLFW_PRESS || context.action() == GLFW.GLFW_REPEAT
                    ? ClientInputApi.Decision.BLOCK
                    : ClientInputApi.Decision.ALLOW;
        });

        ClientInputApi.registerMouseButtonRule(NoellesRolesCore.id("input/timekeeper/rewind_mouse_button"), PRIORITY, context -> {
            if (!TimekeeperRewindInputLock.shouldBlockMouseButton(context.client(), context.button())) {
                return ClientInputApi.Decision.PASS;
            }
            TimekeeperRewindInputLock.releaseBlockedKeys(context.client());
            return ClientInputApi.Decision.BLOCK;
        });

        ClientInputApi.registerMouseScrollRule(NoellesRolesCore.id("input/timekeeper/rewind_mouse_scroll"), PRIORITY, context -> {
            if (!TimekeeperRewindInputLock.isInputLocked(context.client())) {
                return ClientInputApi.Decision.PASS;
            }
            TimekeeperRewindInputLock.releaseBlockedKeys(context.client());
            return ClientInputApi.Decision.BLOCK;
        });

        ClientInputApi.registerInputTickHandler(NoellesRolesCore.id("input/timekeeper/rewind_tick_cleanup"), PRIORITY, context -> {
            if (TimekeeperRewindInputLock.isInputLocked(context.client())) {
                TimekeeperRewindInputLock.releaseBlockedKeys(context.client());
            }
        });

        ClientInputApi.registerActionRule(NoellesRolesCore.id("input/timekeeper/rewind_actions"), PRIORITY, context -> {
            if (!TimekeeperRewindInputLock.isInputLocked(context.client())) {
                return ClientInputApi.ActionResult.pass();
            }
            TimekeeperRewindInputLock.releaseBlockedKeys(context.client());
            return ClientInputApi.ActionResult.block();
        });
    }

    private static boolean shouldRestoreRiftJump(MinecraftClient client, KeyBinding key) {
        if (client.player == null || client.world == null || client.options == null || client.currentScreen != null
                || !key.equals(client.options.jumpKey)) {
            return false;
        }
        return !TimekeeperWorldComponent.KEY.get(client.world).isRewinding()
                && TimekeeperPlayerComponent.KEY.get(client.player).isInTimeRift();
    }

    private static boolean isInRift(MinecraftClient client) {
        return client.player != null && client.options != null
                && TimekeeperPlayerComponent.KEY.get(client.player).isInTimeRift();
    }

    private static boolean matchesHotbarKey(MinecraftClient client, int key, int scanCode) {
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (hotbarKey.matchesKey(key, scanCode)) {
                return true;
            }
        }
        return false;
    }
}
