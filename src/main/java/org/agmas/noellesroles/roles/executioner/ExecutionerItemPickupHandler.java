package org.agmas.noellesroles.roles.executioner;

import dev.doctor4t.wathe.api.item.ItemPickupApi;
import dev.doctor4t.wathe.api.item.ItemPickupContext;
import dev.doctor4t.wathe.api.item.ItemPickupResult;
import dev.doctor4t.wathe.index.tag.WatheItemTags;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 仇杀客拾取枪械的显式放行规则。
 *
 * <p>这条规则只跳过 Wathe 的职业级枪械限制，不会自己复制原版拾取。
 * 实际背包写入、拾取延迟、owner 检查和回放仍由原版方法执行。</p>
 */
public final class ExecutionerItemPickupHandler {
    /** 明确低于重伤/招募等强制禁拾状态，确保这些状态优先。 */
    private static final int PRIORITY = 100;
    private static boolean initialized;

    private ExecutionerItemPickupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ItemPickupApi.registerRule(
                NoellesRolesCore.id("executioner/gun_pickup"),
                PRIORITY,
                ExecutionerItemPickupHandler::resolve
        );
    }

    private static ItemPickupResult resolve(ItemPickupContext context) {
        if (context.stack().isIn(WatheItemTags.GUNS)
                && context.game().isRole(context.player(), NoellesRoleRegistry.EXECUTIONER)
                && !context.player().equals(context.owner())
                && !context.player().getInventory().contains(stack -> stack.isIn(WatheItemTags.GUNS))) {
            return ItemPickupResult.ALLOW;
        }
        return ItemPickupResult.PASS;
    }
}
