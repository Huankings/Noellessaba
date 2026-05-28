package org.agmas.noellesroles.roles.controller;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.NoellesRolesShops;

import java.util.ArrayList;
import java.util.List;

/**
 * 附体师专属杀手商店。
 * 这里完全按需求读取 Wathe 原价，并在少数条目上做差异化调整。
 */
public final class ControllerShopHandler {

    private ControllerShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 武器类：沿用 Wathe 原价，疯魔模式在原价基础上额外 +50。
        entries.add(new ShopEntry(
                WatheItems.KNIFE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100),
                ShopEntry.Type.WEAPON
        ));
        entries.add(new ShopEntry(
                WatheItems.GRENADE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.GRENADE, 300),
                ShopEntry.Type.WEAPON
        ));
        entries.add(new ShopEntry(
                WatheItems.PSYCHO_MODE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.PSYCHO_MODE, 350) + 50,
                ShopEntry.Type.WEAPON
        ));

        // 工具类：除裹尸袋半价外，其余全部跟随 Wathe 原价。
        entries.add(new ShopEntry(
                WatheItems.LOCKPICK.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.LOCKPICK, 50),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                WatheItems.CROWBAR.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.CROWBAR, 25),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                WatheItems.BLACKOUT.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.BLACKOUT, 200),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                WatheItems.BODY_BAG.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.BODY_BAG, 70) / 2,
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
