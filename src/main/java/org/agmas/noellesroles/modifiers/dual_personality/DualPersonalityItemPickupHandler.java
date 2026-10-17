package org.agmas.noellesroles.modifiers.dual_personality;

import dev.doctor4t.wathe.api.item.ItemPickupApi;
import dev.doctor4t.wathe.api.item.ItemPickupContext;
import dev.doctor4t.wathe.api.item.ItemPickupResult;
import dev.doctor4t.wathe.index.WatheItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 双重人格双活阶段的普通左轮禁拾规则。
 *
 * <p>只对活跃对局中被 Wathe `isInnocent` 归为好人的双活人格生效，
 * 因此同时覆盖普通平民和义警阵营，不会误挡其它掉落物。</p>
 */
public final class DualPersonalityItemPickupHandler {
    /** 词条状态禁拾高于仇杀客普通拾枪特权。 */
    private static final int PRIORITY = 150;
    private static boolean initialized;

    private DualPersonalityItemPickupHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        ItemPickupApi.registerRule(
                NoellesRolesCore.id("dual_personality/revolver_pickup"),
                PRIORITY,
                DualPersonalityItemPickupHandler::resolve
        );
    }

    private static ItemPickupResult resolve(ItemPickupContext context) {
        if (!context.stack().isOf(WatheItems.REVOLVER)
                || !DualPersonalityManager.isActiveRound(context.player().getWorld())
                || !DualPersonalityComponent.KEY.get(context.player().getWorld()).isDoubleActive(context.player().getUuid())
                || !context.game().isInnocent(context.player())) {
            return ItemPickupResult.PASS;
        }
        return ItemPickupResult.DENY;
    }
}
