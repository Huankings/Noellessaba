package org.agmas.noellesroles.roles.robber;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;

import java.util.ArrayList;
import java.util.List;

/**
 * 强盗专属商店。
 * 直接按当前工程里其他扩展职业的写法返回一整套覆盖后的条目列表，
 * 避免去改 Wathe 原版商店常量。
 */
public class RobberShopHandler {

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 武器区：先放强盗的核心武器，再放手雷。
        entries.add(new ShopEntry(
                WatheItems.KNIFE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100),
                ShopEntry.Type.WEAPON
        ));
        entries.add(new ShopEntry(
                ModItems.ROBBER_PISTOL.getDefaultStack(),
                Math.max(0, NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, 250) - 120),
                ShopEntry.Type.WEAPON
        ));
        entries.add(new ShopEntry(
                ModItems.THROWING_AXE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100) + 55,
                ShopEntry.Type.WEAPON
        ));
        entries.add(new ShopEntry(
                WatheItems.GRENADE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.GRENADE, 300) + 15,
                ShopEntry.Type.WEAPON
        ));

        // 工具区：全部沿用 Wathe 原价，保证后续基底调价时能自动跟上。
        entries.add(new ShopEntry(
                WatheItems.CROWBAR.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.CROWBAR, 25),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                WatheItems.BODY_BAG.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.BODY_BAG, 70),
                ShopEntry.Type.TOOL
        ));
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
