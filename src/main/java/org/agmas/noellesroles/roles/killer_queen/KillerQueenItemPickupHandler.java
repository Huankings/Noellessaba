package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.item.ItemPickupApi;
import dev.doctor4t.wathe.api.item.ItemPickupContext;
import dev.doctor4t.wathe.api.item.ItemPickupResult;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 杀手女王招募者的左轮禁拾规则。
 *
 * <p>招募流程会强制解除普通左轮；在招募关系结束前，该玩家不能通过踩到
 * ItemEntity 重新获得左轮。priority 300 使它高于仇杀客拾枪特权。</p>
 */
public final class KillerQueenItemPickupHandler {
    /** 招募强制缴械与杰森重伤同层，两者都高于拾枪特权。 */
    private static final int PRIORITY = 300;
    private static boolean initialized;

    private KillerQueenItemPickupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ItemPickupApi.registerRule(
                NoellesRolesCore.id("killer_queen/recruited_revolver_pickup"),
                PRIORITY,
                KillerQueenItemPickupHandler::resolve
        );
    }

    private static ItemPickupResult resolve(ItemPickupContext context) {
        return context.stack().isOf(WatheItems.REVOLVER)
                && KillerQueenWorldComponent.KEY.get(context.player().getWorld()).isRecruited(context.player().getUuid())
                ? ItemPickupResult.DENY
                : ItemPickupResult.PASS;
    }
}
