package org.agmas.noellesroles.roles.winder;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Items;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 风灵师专属商店。
 */
public final class WinderShopHandler {

    private WinderShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 风弹：风灵师基础扰动工具。
        entries.add(new ShopEntry(Items.WIND_CHARGE.getDefaultStack(), 100, ShopEntry.Type.TOOL));

        // 风之印记：风灵师的核心追踪与保命道具。
        entries.add(new ShopEntry(ModItems.WIND_MARK.getDefaultStack(), 100, ShopEntry.Type.TOOL));

        return entries;
    }
}
