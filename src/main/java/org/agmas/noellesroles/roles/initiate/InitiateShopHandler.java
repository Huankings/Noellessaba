package org.agmas.noellesroles.roles.initiate;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.util.Util;
import org.agmas.noellesroles.NoellesRolesShops;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 初学者专属商店。
 *
 * <p>目前只放初学者自己的商品；
 * 后续如果要给其他职业定制商店，也建议按这个文件的结构单独拆分类。</p>
 */
public final class InitiateShopHandler {
    private static final List<ShopEntry> SHOP_ENTRIES = Util.make(new ArrayList<>(), entries -> {
        /*
         * 初学者当前只有一把售价更高的匕首。
         * 价格跟着 Wathe 原价浮动，再额外 +100。
         *
         * StupidExpress 旧实现用 DirectGiveShopEntry 绕过“非杀手不能购买武器”的默认限制。
         * NoellesRoles 的 ShopApi provider 已统一由 NoellesRolesShops.purchase 交付物品，
         * 因此这里保留商品本身和价格即可，不再复制旧的自定义 ShopEntry 类。
         */
        entries.add(new ShopEntry(
                WatheItems.KNIFE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, InitiateConstants.KNIFE_PRICE_FALLBACK)
                        + InitiateConstants.KNIFE_PRICE_BONUS,
                ShopEntry.Type.WEAPON
        ));
    });

    private InitiateShopHandler() {
    }

    public static @NotNull List<ShopEntry> getShopEntries() {
        return SHOP_ENTRIES;
    }
}
