package org.agmas.noellesroles.roles.physician;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.util.Util;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 医师专属商店。
 */
public final class PhysicianShopHandler {
    private PhysicianShopHandler() {
    }

    public static @NotNull List<ShopEntry> getShopEntries() {
        return Util.make(new ArrayList<>(), entries ->
                entries.add(new ShopEntry(ModItems.PILL.getDefaultStack(), PhysicianConstants.PILL_SHOP_PRICE, ShopEntry.Type.POISON)));
    }
}
