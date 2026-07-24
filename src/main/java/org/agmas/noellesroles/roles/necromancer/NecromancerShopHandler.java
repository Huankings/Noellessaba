package org.agmas.noellesroles.roles.necromancer;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.util.ShopEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 死灵法师商店修改器。
 */
public final class NecromancerShopHandler {
    private NecromancerShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.NECROMANCER || NecromancerConstants.HAS_KILLER_SHOP) {
            return;
        }

        /*
         * StupidExpress 默认配置为“死灵法师没有杀手商店”。
         * 这里通过 Wathe ShopApi 直接清空解析后的商品列表，客户端显示和服务端购买校验会保持一致。
         */
        entries.clear();
    }
}
