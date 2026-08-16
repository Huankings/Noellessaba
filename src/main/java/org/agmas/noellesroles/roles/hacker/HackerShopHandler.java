package org.agmas.noellesroles.roles.hacker;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 黑客商店。
 *
 * <p>黑客是中立职业，不能稳定获得杀手任务币。
 * 因此默认杀手商店物品只读取第 0 组金币价格，不复制任务币或多货币条件。</p>
 */
public final class HackerShopHandler {
    private HackerShopHandler() {
    }

    public static @NotNull List<ShopEntry> getShopEntries() {
        if (!HackerConstants.HAS_SHOP) {
            return List.of();
        }

        return Util.make(new ArrayList<>(), entries -> {
            entries.add(new ShopEntry(
                    WatheItems.LOCKPICK.getDefaultStack(),
                    NoellesRolesShops.getItemPrice(WatheItems.LOCKPICK, 50),
                    ShopEntry.Type.TOOL
            ));
            entries.add(new ShopEntry(
                    WatheItems.BLACKOUT.getDefaultStack(),
                    NoellesRolesShops.getItemPrice(WatheItems.BLACKOUT, 250) + 100,
                    ShopEntry.Type.TOOL
            ));
            entries.add(new ShopEntry(ModItems.ICON_WEAPON_COOLDOWN_REFRESH.getDefaultStack(), HackerConstants.REFRESH_WEAPON_COOLDOWN_PRICE, ShopEntry.Type.TOOL));
            entries.add(new ShopEntry(ModItems.ICON_ABILITY_COOLDOWN_REFRESH.getDefaultStack(), HackerConstants.REFRESH_ABILITY_COOLDOWN_PRICE, ShopEntry.Type.TOOL));
            entries.add(new ShopEntry(ModItems.ICON_POTION_EFFECT_REFRESH.getDefaultStack(), HackerConstants.REFRESH_POTION_EFFECT_PRICE, ShopEntry.Type.TOOL));
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
        });
    }
}
