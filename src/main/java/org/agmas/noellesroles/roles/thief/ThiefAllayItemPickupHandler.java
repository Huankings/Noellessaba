package org.agmas.noellesroles.roles.thief;

import dev.doctor4t.wathe.api.item.ItemPickupApi;
import dev.doctor4t.wathe.api.item.ItemPickupContext;
import dev.doctor4t.wathe.api.item.ItemPickupResult;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 精确放行小偷拾取自己悦灵交付的普通左轮。 */
public final class ThiefAllayItemPickupHandler {
    private static boolean initialized;

    private ThiefAllayItemPickupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ItemPickupApi.registerRule(
                NoellesRolesCore.id("thief/allay_revolver_pickup"),
                ThiefConstants.ALLAY_REVOLVER_PICKUP_PRIORITY,
                ThiefAllayItemPickupHandler::resolve
        );
    }

    private static ItemPickupResult resolve(ItemPickupContext context) {
        if (!context.stack().isOf(WatheItems.REVOLVER)
                || !context.game().isRole(context.player(), NoellesRoleRegistry.THIEF)) {
            return ItemPickupResult.PASS;
        }
        String expectedTag = ThiefConstants.ALLAY_DELIVERY_TAG_PREFIX + context.player().getUuid();
        return context.itemEntity().getCommandTags().contains(expectedTag)
                ? ItemPickupResult.ALLOW
                : ItemPickupResult.PASS;
    }
}
