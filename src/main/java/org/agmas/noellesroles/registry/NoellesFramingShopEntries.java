package org.agmas.noellesroles.registry;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.framing.FramingShopEntry;

import java.util.ArrayList;

/**
 * 模仿者、仇杀客、狂信者、梦者共用的伪装商店内容。
 *
 * <p>该列表需要等物品注册完成后再填充，所以保留显式 {@link #init()}；
 * 这样不会因为静态字段提前读取 {@link ModItems} 而打乱物品注册顺序。</p>
 */
public final class NoellesFramingShopEntries {
    public static final ArrayList<ShopEntry> FRAMING_ROLES_SHOP = new ArrayList<>();
    private static boolean initialized = false;

    private NoellesFramingShopEntries() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        FRAMING_ROLES_SHOP.add(new FramingShopEntry(WatheItems.LOCKPICK.getDefaultStack(), 45, ShopEntry.Type.TOOL));
        FRAMING_ROLES_SHOP.add(new FramingShopEntry(ModItems.DELUSION_VIAL.getDefaultStack(), 20, ShopEntry.Type.POISON));
        FRAMING_ROLES_SHOP.add(new FramingShopEntry(WatheItems.FIRECRACKER.getDefaultStack(), 5, ShopEntry.Type.TOOL));
        FRAMING_ROLES_SHOP.add(new FramingShopEntry(WatheItems.NOTE.getDefaultStack(), 5, ShopEntry.Type.TOOL));
    }
}
