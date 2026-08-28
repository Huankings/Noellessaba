package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 巫妖对默认杀手商店的局部改写。
 *
 * <p>巫妖仍属于杀手阵营，所以保留 Wathe 默认杀手商店中没有明确替换的条目。
 * 这里只删除毒药瓶/蝎子，并替换左轮、手雷、疯魔模式三个指定商品位。</p>
 */
public final class LichShopHandler {
    private LichShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.LICH) {
            return;
        }

        removeItem(entries, WatheItems.POISON_VIAL);
        removeItem(entries, WatheItems.SCORPION);

        replaceItem(entries, WatheItems.REVOLVER, new ShopEntry(
                ModItems.ONCE_STAFF.getDefaultStack(),
                Math.max(
                        LichConstants.MIN_SHOP_PRICE,
                        NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, LichConstants.ONCE_STAFF_BASE_PRICE_FALLBACK)
                                - LichConstants.ONCE_STAFF_PRICE_DISCOUNT
                ),
                ShopEntry.Type.WEAPON
        ));
        replaceItem(entries, WatheItems.GRENADE, new ShopEntry(
                ModItems.MAGIC_BARRIER.getDefaultStack(),
                Math.max(
                        LichConstants.MIN_SHOP_PRICE,
                        NoellesRolesShops.getItemPrice(WatheItems.GRENADE, LichConstants.MAGIC_BARRIER_BASE_PRICE_FALLBACK)
                                - LichConstants.MAGIC_BARRIER_PRICE_DISCOUNT
                ),
                ShopEntry.Type.WEAPON
        ));
        replaceItem(entries, WatheItems.PSYCHO_MODE, ShopEntry.action(
                ModItems.PSYCHO_LICH.getDefaultStack(),
                LichConstants.PSYCHO_LICH_PRICE,
                ShopEntry.Type.WEAPON,
                LichPsychoHandler::startLichPsycho
        ));
    }

    private static void removeItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        entries.removeIf(entry -> entry.stack().isOf(item));
    }

    private static void replaceItem(@NotNull List<ShopEntry> entries, @NotNull Item item, @NotNull ShopEntry replacement) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).stack().isOf(item)) {
                entries.set(index, replacement);
                return;
            }
        }
    }
}
