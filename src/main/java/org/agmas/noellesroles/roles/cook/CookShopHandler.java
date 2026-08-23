package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.util.Util;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 厨师专属商店。
 */
public final class CookShopHandler {
    private CookShopHandler() {
    }

    public static @NotNull List<ShopEntry> getShopEntries() {
        return Util.make(new ArrayList<>(), entries -> {
            entries.add(new ShopEntry(ModItems.RANDOM_FOOD.getDefaultStack(), CookConstants.COOKED_FOOD_SHOP_PRICE, ShopEntry.Type.TOOL));
            entries.add(new ShopEntry(ModItems.PAN.getDefaultStack(), CookConstants.PAN_SHOP_PRICE, ShopEntry.Type.WEAPON));
            entries.add(new ShopEntry(ModItems.THROWING_PAN.getDefaultStack(), CookConstants.THROWING_PAN_SHOP_PRICE, ShopEntry.Type.WEAPON));
            entries.add(new ShopEntry(ModItems.PSYCHO_COOK.getDefaultStack(), CookConstants.PSYCHO_COOK_SHOP_PRICE, ShopEntry.Type.WEAPON));
        });
    }
}
