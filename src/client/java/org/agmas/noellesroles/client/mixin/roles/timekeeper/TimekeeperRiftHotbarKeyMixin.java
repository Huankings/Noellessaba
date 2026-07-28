package org.agmas.noellesroles.client.mixin.roles.timekeeper;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时间狭缝期间禁用旁观数字键传送。
 *
 * <p>时间狭缝会把玩家维持在“Wathe 仍视为存活”的 spectator 模式。
 * 原版 spectator 的 1-9 热键会打开/选择旁观目标，并可能瞬间传送到某名玩家身上，
 * 这会让狭缝玩家通过传送位置、视角和目标列表反推出其他玩家身份。</p>
 *
 * <p>这里只拦客户端热键栏按键，不拦普通聊天指令、移动、视角或管理员调试命令。
 * 服务端的狭缝状态仍是权威状态；客户端提前吞键只是为了避免原版旁观 UI 在本地先跳一次。</p>
 */
@Mixin(Keyboard.class)
public class TimekeeperRiftHotbarKeyMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void noellesroles$blockTimeRiftSpectatorHotbarKeys(
            long window,
            int key,
            int scancode,
            int action,
            int modifiers,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null
                || client.player == null
                || client.options == null
                || !TimekeeperPlayerComponent.KEY.get(client.player).isInTimeRift()
                || !noellesroles$isHotbarKey(client, key, scancode)) {
            return;
        }

        /*
         * 如果玩家在进入狭缝前已经按住数字键，KeyBinding 可能仍保持 pressed。
         * 每次命中拦截时都主动清掉热键栏状态，避免后续 handleInputEvents 消费到残留输入。
         * 释放事件本身放行，防止键位在 Minecraft/GLFW 内部状态里卡住。
         */
        noellesroles$releaseHotbarKeys(client);
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            ci.cancel();
        }
    }

    private static boolean noellesroles$isHotbarKey(MinecraftClient client, int key, int scancode) {
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (hotbarKey.matchesKey(key, scancode)) {
                return true;
            }
        }
        return false;
    }

    private static void noellesroles$releaseHotbarKeys(MinecraftClient client) {
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            hotbarKey.setPressed(false);
        }
    }
}
