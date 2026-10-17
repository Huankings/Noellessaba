package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.item.ItemPickupApi;
import dev.doctor4t.wathe.api.item.ItemPickupContext;
import dev.doctor4t.wathe.api.item.ItemPickupResult;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 杰森重伤状态的左轮禁拾规则。
 *
 * <p>重伤时被强制掉落的普通左轮不能立即捡回，但其它物品与其它玩家不受影响。
 * priority 300 明确高于仇杀客的拾枪 ALLOW，保证强制缴械优先。</p>
 */
public final class JasonItemPickupHandler {
    /** 强制缴械层级，明确高于仇杀客 ALLOW 规则。 */
    private static final int PRIORITY = 300;
    private static boolean initialized;

    private JasonItemPickupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ItemPickupApi.registerRule(
                NoellesRolesCore.id("jason/wounded_revolver_pickup"),
                PRIORITY,
                JasonItemPickupHandler::resolve
        );
    }

    private static ItemPickupResult resolve(ItemPickupContext context) {
        return context.stack().isOf(WatheItems.REVOLVER)
                && JasonWoundManager.isWoundedActionLocked(context.player())
                ? ItemPickupResult.DENY
                : ItemPickupResult.PASS;
    }
}
