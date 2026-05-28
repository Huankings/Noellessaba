package org.agmas.noellesroles.roles.bartender;

import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 酒保商店条目。
 */
public final class BartenderShopHandler {

    private BartenderShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 防御药剂：提供一次额外承伤。
        entries.add(new ShopEntry(ModItems.DEFENSE_VIAL.getDefaultStack(), 150, ShopEntry.Type.POISON));

        return entries;
    }
}
