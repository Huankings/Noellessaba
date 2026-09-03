package org.agmas.noellesroles.roles.waiter;

import dev.doctor4t.wathe.api.tray.TrayTakeRegistry;
import dev.doctor4t.wathe.api.tray.TrayTakeDecision;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 服务员托盘同类物品最多持有两份的公共 API 接入。 */
public final class WaiterTrayTakeRules {
    private WaiterTrayTakeRules() {
    }

    public static void init() {
        TrayTakeRegistry.registerGroupRule("noellesroles:waiter", 100, context -> {
            if (!GameWorldComponent.KEY.get(context.player().getWorld()).isRole(context.player(), NoellesRoleRegistry.WAITER)) {
                return null;
            }
            String itemId = net.minecraft.registry.Registries.ITEM.getId(context.candidate().getItem()).toString();
            return new TrayTakeDecision("noellesroles:waiter:" + itemId, WaiterConstants.MAX_TRAY_ITEM_COUNT, TrayTakeDecision.Mode.TOTAL_COUNT);
        });
    }
}
