package org.agmas.noellesroles.shop;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.NoellesRolesShops;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 角色商店提供器。
 *
 * <p>以后无论是“固定商品列表”，还是“根据阶段/状态动态变化的商品列表”，
 * 都统一实现这一层即可，不需要再重复写一套购买 mixin 和界面 mixin。</p>
 */
public interface RoleShopProvider {

    /**
     * 获取当前玩家此刻应该看到的商店条目。
     */
    @NotNull List<ShopEntry> getShopEntries(@NotNull PlayerEntity player);

    /**
     * 默认购买处理：走 NoellesRoles 统一购买逻辑。
     *
     * <p>这样可以保留 BLACKOUT、PSYCHO_MODE、电力恢复系统等特殊购买行为。</p>
     */
    default boolean handlePurchase(@NotNull PlayerEntity player, int balance, @NotNull ShopEntry entry) {
        return NoellesRolesShops.handlePurchase(player, balance, entry.stack(), entry.price());
    }

    /**
     * 默认结算处理：扣钱并同步组件。
     */
    default void completePurchase(@NotNull PlayerShopComponentAccessor shop, int price) {
        NoellesRolesShops.completePurchase(shop, price);
    }
}
