package org.agmas.noellesroles.client.roles.engineer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.mixin.roles.timekeeper.TimekeeperKeyBindingAccessor;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;

/**
 * 眩晕 / 定身期间的客户端输入锁。
 *
 * <p>服务端的 {@link StunnedPlayerComponent} 才是眩晕状态的权威来源；这里做的是客户端体验兜底：
 * Minecraft 的 {@link KeyBinding} 内部同时保存“当前是否按住”的 pressed 和“短按待消费次数”的
 * timesPressed。如果定身期间只把 wasPressed()/isPressed() 的返回值改成 false，
 * 玩家按下的 E、左右键、数字键等仍可能累积在内部队列里，等定身同步结束后被 handleInputEvents
 * 一次性消费，表现为自动打开背包、补攻击、补右键蓄力释放。</p>
 *
 * <p>因此，被眩晕时凡是会改变局内运行态的键，都必须同时清 pressed 与 timesPressed。
 * 移动方向键只在读取时返回 false，不在这里主动 setPressed(false)：如果玩家定身结束时仍按着 W，
 * 保留物理按住状态可以让移动自然恢复，不需要松开再按一次。</p>
 */
public final class StunnedInputLock {
    private StunnedInputLock() {
    }

    public static boolean isInputLocked(MinecraftClient client) {
        return client != null
                && client.player != null
                && StunnedPlayerComponent.KEY.get(client.player).isStunned();
    }

    public static boolean shouldBlockKey(MinecraftClient client, KeyBinding key) {
        return isInputLocked(client) && isStunnedBlockedKey(client, key);
    }

    public static boolean shouldBlockMouseButton(MinecraftClient client, int button) {
        if (!isInputLocked(client) || client.options == null) {
            return false;
        }

        if (client.options.attackKey.matchesMouse(button)
                || client.options.useKey.matchesMouse(button)
                || client.options.pickItemKey.matchesMouse(button)
                || client.options.swapHandsKey.matchesMouse(button)
                || client.options.dropKey.matchesMouse(button)
                || client.options.inventoryKey.matchesMouse(button)
                || client.options.jumpKey.matchesMouse(button)
                || client.options.sneakKey.matchesMouse(button)
                || client.options.sprintKey.matchesMouse(button)
                || (NoellesrolesClient.abilityBind != null && NoellesrolesClient.abilityBind.matchesMouse(button))) {
            return true;
        }

        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (hotbarKey.matchesMouse(button)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldBlockKeyboardPress(MinecraftClient client, int key, int scancode) {
        if (!isInputLocked(client) || client.options == null || client.currentScreen != null) {
            return false;
        }

        /*
         * 键盘事件层只拦“离散操作键”：背包、攻击、使用、丢弃、副手、选槽和 Noelles 能力键。
         * W/A/S/D、跳跃、潜行、疾跑这类持续键仍交给 KeyBinding 读值时返回 false，
         * 这样定身结束时如果玩家还按着移动类键，原版 pressed 状态仍能自然恢复。
         */
        if (client.options.attackKey.matchesKey(key, scancode)
                || client.options.useKey.matchesKey(key, scancode)
                || client.options.pickItemKey.matchesKey(key, scancode)
                || client.options.swapHandsKey.matchesKey(key, scancode)
                || client.options.dropKey.matchesKey(key, scancode)
                || client.options.inventoryKey.matchesKey(key, scancode)
                || (NoellesrolesClient.abilityBind != null && NoellesrolesClient.abilityBind.matchesKey(key, scancode))) {
            return true;
        }

        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            if (hotbarKey.matchesKey(key, scancode)) {
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
         * 这些键会触发攻击、使用、开背包、切槽、丢弃、副手交换、能力等离散动作。
         * 它们最容易在 timesPressed 中积攒“定身期间按过几次”，所以每次检测到眩晕输入锁时都归零。
         */
        releaseKey(client.options.attackKey);
        releaseKey(client.options.useKey);
        releaseKey(client.options.pickItemKey);
        releaseKey(client.options.swapHandsKey);
        releaseKey(client.options.dropKey);
        releaseKey(client.options.inventoryKey);
        if (NoellesrolesClient.abilityBind != null) {
            releaseKey(NoellesrolesClient.abilityBind);
        }
        for (KeyBinding hotbarKey : client.options.hotbarKeys) {
            releaseKey(hotbarKey);
        }

        /*
         * 跳跃、潜行、疾跑虽然通常是持续状态，但也可能被其它客户端逻辑以 wasPressed 方式读取。
         * 定身状态下它们不应该在结束后补一次短按，因此同样清队列。
         * 这里不清它们的 pressed，也不清 W/A/S/D 移动方向键：
         * 定身期间 KeyBinding mixin 会把读取结果改成 false；定身结束时如果玩家仍按着这些持续键，
         * 保留物理按住状态可以让移动 / 跳跃 / 潜行 / 疾跑自然恢复，不需要松开再按一次。
         */
        clearPressQueue(client.options.jumpKey);
        clearPressQueue(client.options.sneakKey);
        clearPressQueue(client.options.sprintKey);

        /*
         * 玩家可能在被定身前或定身开始那一帧已经处于右键蓄力 / 持续使用物品状态。
         * clearActiveItem() 只清客户端“正在使用”的标记，不走 stopUsingItem() 的松手释放链路，
         * 因此不会额外触发平底锅、枪械、投掷物或其它蓄力物品的完成效果。
         */
        if (client.player != null && client.player.isUsingItem()) {
            client.player.clearActiveItem();
        }
    }

    private static boolean isStunnedBlockedKey(MinecraftClient client, KeyBinding key) {
        if (client.options == null) {
            return false;
        }

        if (key.equals(client.options.forwardKey)
                || key.equals(client.options.backKey)
                || key.equals(client.options.leftKey)
                || key.equals(client.options.rightKey)
                || key.equals(client.options.jumpKey)
                || key.equals(client.options.sneakKey)
                || key.equals(client.options.sprintKey)
                || key.equals(client.options.attackKey)
                || key.equals(client.options.useKey)
                || key.equals(client.options.pickItemKey)
                || key.equals(client.options.swapHandsKey)
                || key.equals(client.options.dropKey)
                || key.equals(client.options.inventoryKey)
                || (NoellesrolesClient.abilityBind != null && key.equals(NoellesrolesClient.abilityBind))) {
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
        clearPressQueue(key);
    }

    private static void clearPressQueue(KeyBinding key) {
        ((TimekeeperKeyBindingAccessor) key).noellesroles$setTimesPressed(0);
    }
}
