package org.agmas.noellesroles.roles.coward;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;

import java.util.ArrayList;
import java.util.List;

/**
 * 胆小鬼商店。
 */
public final class CowardShopHandler {
    private CowardShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();
    //    这里让胆小鬼的左轮手枪来源于捡别人的，而不是自己买的，因此商店仅出售镇静试剂
    //    entries.add(new ShopEntry(
    //            WatheItems.REVOLVER.getDefaultStack(),
    //            NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, 250),
    //            ShopEntry.Type.WEAPON
    //    ));
        entries.add(new ShopEntry(ModItems.SEDATIVE.getDefaultStack(), CowardConstants.SEDATIVE_PRICE, ShopEntry.Type.POISON));
        return entries;
    }
}
