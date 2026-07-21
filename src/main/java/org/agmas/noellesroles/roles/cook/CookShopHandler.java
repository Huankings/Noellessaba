package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Items;
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
            entries.add(new ShopEntry(ModItems.PAN.getDefaultStack(), CookConstants.PAN_SHOP_PRICE, ShopEntry.Type.WEAPON));
            entries.add(new ShopEntry(Items.COOKED_BEEF.getDefaultStack(), CookConstants.COOKED_FOOD_SHOP_PRICE, ShopEntry.Type.POISON));
            entries.add(new ShopEntry(Items.COOKED_CHICKEN.getDefaultStack(), CookConstants.COOKED_FOOD_SHOP_PRICE, ShopEntry.Type.POISON));
            entries.add(new ShopEntry(Items.COOKED_PORKCHOP.getDefaultStack(), CookConstants.COOKED_FOOD_SHOP_PRICE, ShopEntry.Type.POISON));
        });
    }
}
