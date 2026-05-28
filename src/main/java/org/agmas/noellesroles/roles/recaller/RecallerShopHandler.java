package org.agmas.noellesroles.roles.recaller;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 回溯者商店条目。
 */
public final class RecallerShopHandler {

    private RecallerShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 末影珍珠：快速位移。
        entries.add(new ShopEntry(Items.ENDER_PEARL.getDefaultStack(), 125, ShopEntry.Type.TOOL));
        // 紫颂果：随机脱身。
        entries.add(new ShopEntry(Items.CHORUS_FRUIT.getDefaultStack(), 75, ShopEntry.Type.TOOL));

        return entries;
    }
}
