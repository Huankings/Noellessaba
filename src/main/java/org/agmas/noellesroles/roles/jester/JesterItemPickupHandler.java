package org.agmas.noellesroles.roles.jester;

import dev.doctor4t.wathe.api.item.ItemPickupApi;
import dev.doctor4t.wathe.api.item.ItemPickupContext;
import dev.doctor4t.wathe.api.item.ItemPickupResult;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 狂信者的全部掉落物禁拾规则。
 *
 * <p>保留旧 Mixin 的精确语义：只要仍是 Jester 且不是创造模式，
 * 就拒绝任何普通 ItemEntity，不只针对枪械。</p>
 */
public final class JesterItemPickupHandler {
    private static final int PRIORITY = 200;
    private static boolean initialized;

    private JesterItemPickupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ItemPickupApi.registerRule(
                NoellesRolesCore.id("jester/item_pickup"),
                PRIORITY,
                JesterItemPickupHandler::resolve
        );
    }

    private static ItemPickupResult resolve(ItemPickupContext context) {
        // 创造管理员仍保留现场调试和回收掉落物的能力。
        if (!context.player().isCreative()
                && context.game().isRole(context.player(), NoellesRoleRegistry.JESTER)) {
            return ItemPickupResult.DENY;
        }
        return ItemPickupResult.PASS;
    }
}
