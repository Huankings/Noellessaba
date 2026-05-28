package org.agmas.noellesroles.roles.coroner;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

public class CoronerShopHandler {

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 便宜工具与伪装道具。
        entries.add(new ShopEntry(WatheItems.BODY_BAG.getDefaultStack(), 55, ShopEntry.Type.TOOL));
        entries.add(new ShopEntry(new ItemStack(WatheItems.NOTE, 4), 15, ShopEntry.Type.TOOL));
        entries.add(new ShopEntry(WatheItems.FIRECRACKER.getDefaultStack(), 20, ShopEntry.Type.TOOL));

        // 昂贵武器和工具。
        entries.add(new ShopEntry(WatheItems.LOCKPICK.getDefaultStack(), 100, ShopEntry.Type.TOOL));
        entries.add(new ShopEntry(WatheItems.KNIFE.getDefaultStack(), 150, ShopEntry.Type.WEAPON));
        entries.add(new ShopEntry(WatheItems.REVOLVER.getDefaultStack(), 250, ShopEntry.Type.WEAPON));
        entries.add(new ShopEntry(WatheItems.GRENADE.getDefaultStack(), 400, ShopEntry.Type.WEAPON));

        return entries;
    }
}
