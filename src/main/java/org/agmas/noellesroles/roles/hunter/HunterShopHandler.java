package org.agmas.noellesroles.roles.hunter;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 追猎者杀手商店修改器。
 *
 * <p>追猎者保留 Wathe 默认杀手商店的大部分商品，只删掉毒物、插入猎刀并调整普通匕首价格。</p>
 */
public final class HunterShopHandler {
    private HunterShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.HUNTER) {
            return;
        }

        // 旧追猎者商店没有毒药瓶和蝎子；其它默认杀手工具继续保留。
        removeItem(entries, WatheItems.POISON_VIAL);
        removeItem(entries, WatheItems.SCORPION);

        int defaultKnifePrice = NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100);
        int knifeIndex = indexOfItem(entries, WatheItems.KNIFE);
        int insertIndex = knifeIndex >= 0 ? knifeIndex : 0;

        // 猎刀插在普通匕首前，价格沿用默认匕首，方便追猎者优先购买自己的核心武器。
        entries.add(insertIndex, new ShopEntry(
                ModItems.HUNTING_KNIFE.getDefaultStack(),
                defaultKnifePrice,
                ShopEntry.Type.WEAPON
        ));

        // 普通匕首仍可购买，但价格提高为默认价的 7/4，和 kinssaba 追猎者商店保持一致。
        replaceItem(entries, WatheItems.KNIFE, new ShopEntry(
                WatheItems.KNIFE.getDefaultStack(),
                defaultKnifePrice * HunterConstants.NORMAL_KNIFE_PRICE_NUMERATOR / HunterConstants.NORMAL_KNIFE_PRICE_DENOMINATOR,
                ShopEntry.Type.WEAPON
        ));
    }

    private static void removeItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        entries.removeIf(entry -> entry.stack().isOf(item));
    }

    private static void replaceItem(@NotNull List<ShopEntry> entries, @NotNull Item item, @NotNull ShopEntry replacement) {
        int index = indexOfItem(entries, item);
        if (index >= 0) {
            entries.set(index, replacement);
        }
    }

    private static int indexOfItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).stack().isOf(item)) {
                return i;
            }
        }
        return -1;
    }
}
