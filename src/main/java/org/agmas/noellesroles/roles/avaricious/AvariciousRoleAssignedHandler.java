package org.agmas.noellesroles.roles.avaricious;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import net.minecraft.entity.player.PlayerEntity;

/**
 * 扒手职业分配初始化。
 */
public final class AvariciousRoleAssignedHandler {
    private AvariciousRoleAssignedHandler() {
    }

    public static void onRoleAssigned(PlayerEntity player, Role role) {
        if (role != NoellesRoleRegistry.AVARICIOUS) {
            return;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        shop.setBalance(AvariciousConstants.STARTING_BALANCE);

        /*
         * 扒手收益 HUD 要和服务端真实发钱点严格对齐。
         * 每次分配到扒手时清掉世界级起点，下一次服务端结算 tick 会重新记录并同步。
         */
        AvariciousPayoutComponent.KEY.get(player.getWorld()).reset();
    }
}
