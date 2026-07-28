package org.agmas.noellesroles.client.roles.timekeeper;

import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.agmas.noellesroles.client.mixin.roles.timekeeper.TimekeeperKeyBindingAccessor;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;

/**
 * 时间狭缝期间的客户端能力/背包输入锁。
 *
 * <p>时间狭缝玩家处于“特殊存活旁观”：他们不能发言、不能听言、不能读其它玩家身份，
 * 但客户端仍可能因为 Wathe 认为其“玩法上存活”而保留 NoellesRoles 的能力键和 LimitedInventory 背包入口。
 * 这里专门锁住 G 键能力和 E 键背包，避免狭缝玩家在等待回溯的 30 秒内使用职业能力或背包按钮。</p>
 *
 * <p>不要把这个锁和回溯播放输入锁合并：
 * 回溯播放锁处理的是“未受保护玩家正在被快照倒放”时的攻击/使用残留输入；
 * 本类处理的是“已经死亡但暂时留在时间狭缝”的信息隔离。它不锁移动、视角、跳跃、聊天或指令输入。</p>
 */
public final class TimekeeperRiftInputLock {
    private TimekeeperRiftInputLock() {
    }

    public static boolean shouldBlockKey(MinecraftClient client, KeyBinding key) {
        if (client == null
                || client.player == null
                || client.options == null
                || !TimekeeperPlayerComponent.KEY.get(client.player).isInTimeRift()) {
            return false;
        }

        return key.equals(client.options.inventoryKey)
                || (NoellesrolesClient.abilityBind != null && key.equals(NoellesrolesClient.abilityBind));
    }

    public static void releaseBlockedKeys(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }

        /*
         * KeyBinding 内部同时有 pressed 和 timesPressed 两层状态。
         * 只把返回值改成 false，下一帧仍可能消费到进入狭缝前积攒的 wasPressed 计数；
         * 因此这里像回溯输入锁一样，把两枚被禁用的键都主动清空。
         */
        releaseKey(client.options.inventoryKey);
        if (NoellesrolesClient.abilityBind != null) {
            releaseKey(NoellesrolesClient.abilityBind);
        }

        /*
         * 玩家可能在死亡进入狭缝的瞬间已经打开了 Wathe 的 LimitedInventory。
         * 仅禁止 E 键只能阻止“之后打开”，不能阻止已经打开的背包按钮继续点击；
         * 所以一旦发现狭缝期间仍停留在 LimitedInventoryScreen，就立刻关闭。
         * 这里只关 Wathe 局内背包，不关聊天/命令界面，保证管理员调试入口不受影响。
         */
        if (client.currentScreen instanceof LimitedInventoryScreen) {
            client.setScreen(null);
        }
    }

    private static void releaseKey(KeyBinding key) {
        key.setPressed(false);
        ((TimekeeperKeyBindingAccessor) key).noellesroles$setTimesPressed(0);
    }
}
