package org.agmas.noellesroles.client.roles.timekeeper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.mixin.roles.timekeeper.TimekeeperKeyBindingAccessor;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;

/**
 * 时间回溯期间的客户端输入锁。
 *
 * <p>未购买并消费“回溯保护”的玩家会被服务端冻结和快照回滚。
 * 但 Minecraft 客户端本地还会保存 attack/use 的 pressed 状态与 wasPressed 待消费次数；
 * 如果只在服务端冻结，这些残留输入会在回溯结束后被当成一串短按重新消费，
 * 表现为 Derringer/REVOLVER 空枪连响、长蓄力物品松手后又触发一次等问题。</p>
 *
 * <p>这里不拦聊天键、命令输入或 GUI 文本输入，只处理游戏世界里的攻击、使用、选槽、
 * 丢弃和副手交换等会改变运行态的输入。受回溯保护的玩家不进入本锁，仍按保护语义自由操作。</p>
 */
public final class TimekeeperRewindInputLock {
    private TimekeeperRewindInputLock() {
    }

    public static boolean isInputLocked(MinecraftClient client) {
        if (client == null || client.player == null || client.world == null) {
            return false;
        }

        TimekeeperWorldComponent worldComponent = TimekeeperWorldComponent.KEY.get(client.world);
        return worldComponent.isRewinding()
                && !worldComponent.isProtectedFromCurrentRewind(client.player.getUuid());
    }

    public static boolean shouldBlockKey(MinecraftClient client, KeyBinding key) {
        return isInputLocked(client) && isGameplayMutationKey(client, key);
    }

    public static boolean shouldBlockMouseButton(MinecraftClient client, int button) {
        if (!isInputLocked(client) || client.options == null) {
            return false;
        }

        if (client.options.attackKey.matchesMouse(button)
                || client.options.useKey.matchesMouse(button)
                || client.options.pickItemKey.matchesMouse(button)
                || client.options.swapHandsKey.matchesMouse(button)
                || client.options.dropKey.matchesMouse(button)) {
            return true;
        }

        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (hotbarKey.matchesMouse(button)) {
                return true;
            }
        }
        return false;
    }

    public static void releaseBlockedKeys(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }

        /*
         * setPressed(false) 只清“当前是否按住”；timesPressed 仍可能积累了一批待消费短按。
         * 因此每个被锁的键都同时归零 pressed 和 timesPressed，避免回溯结束后 handleInputEvents
         * 把旧输入一帧帧吐出来。
         */
        releaseKey(client.options.attackKey);
        releaseKey(client.options.useKey);
        releaseKey(client.options.pickItemKey);
        releaseKey(client.options.swapHandsKey);
        releaseKey(client.options.dropKey);
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            releaseKey(hotbarKey);
        }

        /*
         * 玩家可能在回溯开始前已经处于右键蓄力/持续使用状态。
         * clearActiveItem() 是纯客户端状态清理，不会像 stopUsingItem() 一样走“松手释放”链路，
         * 所以不会额外发射枪械、投掷物或触发蓄力完成。
         */
        if (client.player != null && client.player.isUsingItem()) {
            client.player.clearActiveItem();
        }
    }

    private static boolean isGameplayMutationKey(MinecraftClient client, KeyBinding key) {
        if (client.options == null) {
            return false;
        }

        if (key.equals(client.options.attackKey)
                || key.equals(client.options.useKey)
                || key.equals(client.options.pickItemKey)
                || key.equals(client.options.swapHandsKey)
                || key.equals(client.options.dropKey)) {
            return true;
        }

        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (key.equals(hotbarKey)) {
                return true;
            }
        }
        return false;
    }

    private static void releaseKey(KeyBinding key) {
        key.setPressed(false);
        ((TimekeeperKeyBindingAccessor) key).noellesroles$setTimesPressed(0);
    }
}
