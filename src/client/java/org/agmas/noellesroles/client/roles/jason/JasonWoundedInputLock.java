package org.agmas.noellesroles.client.roles.jason;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.mixin.roles.timekeeper.TimekeeperKeyBindingAccessor;
import org.agmas.noellesroles.roles.jason.JasonWoundedPlayerComponent;

/**
 * 杰森重伤倒地的客户端窄输入锁。
 *
 * <p>用户要求倒地期间不能攻击、使用、跳跃、疾跑或按能力键，但仍然可以整理背包。
 * 所以这里刻意不屏蔽背包、热键、丢弃、副手交换、选取方块或滚轮，只处理指定的局内动作。</p>
 */
public final class JasonWoundedInputLock {
    private JasonWoundedInputLock() {
    }

    public static boolean isInputLocked(MinecraftClient client) {
        return client != null
                && client.player != null
                && GameFunctions.isPlayerAliveAndSurvival(client.player)
                && JasonWoundedPlayerComponent.KEY.get(client.player).isWounded();
    }

    public static boolean shouldBlockKey(MinecraftClient client, KeyBinding key) {
        return isInputLocked(client) && isBlockedKey(client, key);
    }

    public static boolean shouldBlockMouseButton(MinecraftClient client, int button) {
        if (!isInputLocked(client) || client.options == null || client.currentScreen != null) {
            return false;
        }
        return client.options.attackKey.matchesMouse(button)
                || client.options.useKey.matchesMouse(button)
                || (NoellesrolesClient.abilityBind != null && NoellesrolesClient.abilityBind.matchesMouse(button));
    }

    public static boolean shouldBlockKeyboardPress(MinecraftClient client, int key, int scancode) {
        if (!isInputLocked(client) || client.options == null || client.currentScreen != null) {
            return false;
        }
        return client.options.attackKey.matchesKey(key, scancode)
                || client.options.useKey.matchesKey(key, scancode)
                || client.options.jumpKey.matchesKey(key, scancode)
                || client.options.sprintKey.matchesKey(key, scancode)
                || (NoellesrolesClient.abilityBind != null && NoellesrolesClient.abilityBind.matchesKey(key, scancode));
    }

    public static void releaseBlockedKeys(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }

        /*
         * 攻击、使用和能力键是离散动作，必须同时清 pressed 与 timesPressed，
         * 否则倒地结束后一帧可能补执行一次此前按下的操作。
         */
        releaseKey(client.options.attackKey);
        releaseKey(client.options.useKey);
        if (NoellesrolesClient.abilityBind != null) {
            releaseKey(NoellesrolesClient.abilityBind);
        }

        /*
         * 跳跃和疾跑是持续键。这里只清短按队列，不清物理 pressed，
         * 因此被救起后如果玩家仍按住按键，移动会自然恢复，不需要重新松开再按。
         */
        clearPressQueue(client.options.jumpKey);
        clearPressQueue(client.options.sprintKey);

        if (client.player != null && client.player.isUsingItem()) {
            // 清客户端使用状态，不走松手释放链路，防止蓄力投掷物在倒地时被意外放出。
            client.player.clearActiveItem();
        }
    }

    private static boolean isBlockedKey(MinecraftClient client, KeyBinding key) {
        if (client.options == null) {
            return false;
        }
        return key.equals(client.options.attackKey)
                || key.equals(client.options.useKey)
                || key.equals(client.options.jumpKey)
                || key.equals(client.options.sprintKey)
                || (NoellesrolesClient.abilityBind != null && key.equals(NoellesrolesClient.abilityBind));
    }

    private static void releaseKey(KeyBinding key) {
        key.setPressed(false);
        clearPressQueue(key);
    }

    private static void clearPressQueue(KeyBinding key) {
        ((TimekeeperKeyBindingAccessor) key).noellesroles$setTimesPressed(0);
    }
}
