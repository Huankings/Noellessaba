package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.item.ItemPickupApi;
import dev.doctor4t.wathe.api.item.ItemPickupContext;
import dev.doctor4t.wathe.api.item.ItemPickupResult;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 执照恶棍存活时的全部掉落物禁拾规则。
 *
 * <p>只对 Wathe 认定的局内存活玩家生效；死亡旁观、创造调试或特殊存活状态
 * 由 `GameFunctions.isPlayerAliveAndSurvival` 统一决定，避免组件残留造成局外禁拾。</p>
 */
public final class LicensedVillainItemPickupHandler {
    private static final int PRIORITY = 200;
    private static boolean initialized;

    private LicensedVillainItemPickupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ItemPickupApi.registerRule(
                NoellesRolesCore.id("licensed_villain/item_pickup"),
                PRIORITY,
                LicensedVillainItemPickupHandler::resolve
        );
    }

    private static ItemPickupResult resolve(ItemPickupContext context) {
        if (GameFunctions.isPlayerAliveAndSurvival(context.player())
                && context.game().isRole(context.player(), NoellesRoleRegistry.LICENSED_VILLAIN)) {
            return ItemPickupResult.DENY;
        }
        return ItemPickupResult.PASS;
    }
}
