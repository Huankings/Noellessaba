package org.agmas.noellesroles.roles.assassin;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;

import java.util.ArrayList;
import java.util.List;

/**
 * 刺客专属商店。
 */
public final class AssassinShopHandler {

    private AssassinShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        entries.add(new ShopEntry(
                ModItems.BAYONET.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100),
                ShopEntry.Type.WEAPON
        ));
        entries.add(new ShopEntry(
                WatheItems.LOCKPICK.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.LOCKPICK, 50),
                ShopEntry.Type.TOOL
        ));
    // 暂时注释无声手枪的购买
    //    entries.add(new ShopEntry(
    //            ModItems.SILENCED_REVOLVER.getDefaultStack(),
    //            NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, 250),
    //            ShopEntry.Type.WEAPON
    //    ));
        entries.add(new ShopEntry(
                ModItems.BAYONET_COLDOWN_REFRESH.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100) + 50,
                ShopEntry.Type.TOOL
        ));
    //暂时注释无声手雷的购买，后面有机会再开启
    //    entries.add(new ShopEntry(
    //            ModItems.SILENT_GRENADE.getDefaultStack(),
    //            NoellesRolesShops.getItemPrice(WatheItems.GRENADE, 300) + 50,
    //            ShopEntry.Type.WEAPON
    //    ));
        entries.add(new ShopEntry(
                WatheItems.BLACKOUT.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.BLACKOUT, 200),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                WatheItems.FIRECRACKER.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.FIRECRACKER, 10),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                new ItemStack(WatheItems.NOTE, 4),
                NoellesRolesShops.getItemPrice(WatheItems.NOTE, 10),
                ShopEntry.Type.TOOL
        ));

        return entries;
    }
}
