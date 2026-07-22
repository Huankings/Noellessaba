package org.agmas.noellesroles.roles.drugmaker;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 制毒师杀手商店修改器。
 *
 * <p>制毒师不是一张完全独立商店，而是在 Wathe 默认杀手商店上做局部改写：
 * 删除爆发武器、插入专属毒具，并调整普通刀和毒物价格。</p>
 */
public final class DrugmakerShopHandler {
    private DrugmakerShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != Noellesroles.DRUGMAKER) {
            return;
        }

        removeItem(entries, WatheItems.REVOLVER);
        removeItem(entries, WatheItems.GRENADE);
        removeItem(entries, WatheItems.PSYCHO_MODE);

        /*
         * 两个专属毒具都插到匕首前，最终顺序保持为：
         * POISON_INJECTOR -> BLOWGUN -> KNIFE。
         */
        insertBeforeItem(entries, WatheItems.KNIFE, new ShopEntry(
                ModItems.POISON_INJECTOR.getDefaultStack(),
                DrugmakerConstants.POISON_INJECTOR_PRICE,
                ShopEntry.Type.WEAPON
        ));
        insertBeforeItem(entries, WatheItems.KNIFE, new ShopEntry(
                ModItems.BLOWGUN.getDefaultStack(),
                DrugmakerConstants.BLOWGUN_PRICE,
                ShopEntry.Type.WEAPON
        ));

        replaceItem(entries, WatheItems.KNIFE, new ShopEntry(
                WatheItems.KNIFE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100) * 2,
                ShopEntry.Type.WEAPON
        ));
        replaceItem(entries, WatheItems.POISON_VIAL, new ShopEntry(
                WatheItems.POISON_VIAL.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.POISON_VIAL, 70) / 2,
                ShopEntry.Type.POISON
        ));
        replaceItem(entries, WatheItems.SCORPION, new ShopEntry(
                WatheItems.SCORPION.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.SCORPION, 40) / 2,
                ShopEntry.Type.POISON
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

    private static void insertBeforeItem(@NotNull List<ShopEntry> entries, @NotNull Item item, @NotNull ShopEntry entry) {
        int index = indexOfItem(entries, item);
        entries.add(index >= 0 ? index : 0, entry);
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
