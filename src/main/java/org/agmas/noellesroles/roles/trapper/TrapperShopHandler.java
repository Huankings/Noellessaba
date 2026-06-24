package org.agmas.noellesroles.roles.trapper;

import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 调查官商店条目。
 */
public final class TrapperShopHandler {

    private TrapperShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 身份检测装置：放置后侦测经过玩家身份。
        entries.add(new ShopEntry(ModItems.ROLE_MINE.getDefaultStack(), 125, ShopEntry.Type.POISON));

        return entries;
    }
}
