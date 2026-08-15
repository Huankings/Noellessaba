package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.mixin.roles.timekeeper.TimekeeperKeyBindingAccessor;
import org.agmas.noellesroles.roles.jason.JasonAbilityRules;
import org.lwjgl.glfw.GLFW;

/**
 * 无恶不在期间的客户端输入锁。
 *
 * <p>用户补充确认：这里只锁物理鼠标左/右键和背包 E 键，不锁其它键。
 * 倒地锁仍然可以继续管理跳跃、疾跑和能力键；本类只负责无恶不在自己的更窄输入限制。</p>
 */
public final class JasonAbilityInputLock {
    private JasonAbilityInputLock() {
    }

    public static boolean isInputLocked(MinecraftClient client) {
        return client != null
                && client.player != null
                && GameFunctions.isPlayerAliveAndSurvival(client.player)
                && JasonAbilityRules.isAbilityActiveLike(client.player);
    }

    public static boolean shouldBlockKey(MinecraftClient client, KeyBinding key) {
        return isInputLocked(client) && shouldBlockInventoryKey(client, key);
    }

    public static boolean shouldBlockMouseButton(MinecraftClient client, int button) {
        if (!isInputLocked(client) || client.currentScreen != null) {
            return false;
        }

        /*
         * 无恶不在只吞“物理鼠标左右键”。
         * 不按攻击/使用 KeyBinding 判断，是为了避免玩家把其它鼠标键或其它功能绑定成攻击/使用后，
         * 这些额外输入在客户端事件层被一并锁死；真正的攻击/交互效果仍由 MinecraftClient 和服务端兜底取消。
         */
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    public static boolean shouldBlockKeyboardPress(MinecraftClient client, int key, int scancode) {
        if (!isInputLocked(client) || client.options == null || client.currentScreen != null) {
            return false;
        }
        return client.options.inventoryKey.matchesKey(key, scancode);
    }

    public static void releaseBlockedKeys(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }

        releaseKey(client.options.inventoryKey);

        /*
         * 无恶不在期间如果已经打开了 Wathe 的 LimitedInventory，必须立刻关闭。
         * 这里只处理背包屏，不关聊天、命令或管理员调试界面。
         */
        if (client.currentScreen instanceof LimitedInventoryScreen) {
            client.setScreen(null);
        }
    }

    private static boolean shouldBlockInventoryKey(MinecraftClient client, KeyBinding key) {
        if (client.options == null) {
            return false;
        }
        return key.equals(client.options.inventoryKey);
    }

    private static void releaseKey(KeyBinding key) {
        key.setPressed(false);
        ((TimekeeperKeyBindingAccessor) key).noellesroles$setTimesPressed(0);
    }
}
