package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 执照恶棍专属商店。
 */
public final class LicensedVillainShopHandler {
    private static final List<ShopEntry> SHOP_ENTRIES = Util.make(new ArrayList<>(), entries ->
            entries.add(new ShopEntry(
                    WatheItems.REVOLVER.getDefaultStack(),
                    LicensedVillainConstants.REVOLVER_PRICE,
                    ShopEntry.Type.WEAPON
            )));

    private LicensedVillainShopHandler() {
    }

    public static @NotNull List<ShopEntry> getShopEntries() {
        /*
         * 这里只描述“卖什么”。实际购买、扣钱、音效和回放继续交给 Wathe ShopApi
         * 与 NoellesRolesShops.purchase，避免复制旧 mixin 里的商店结算流程。
         */
        return SHOP_ENTRIES;
    }
}
