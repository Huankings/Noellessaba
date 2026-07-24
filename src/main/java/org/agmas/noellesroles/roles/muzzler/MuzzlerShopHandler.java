package org.agmas.noellesroles.roles.muzzler;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 静语者商店修改器。
 *
 * <p>只把默认杀手商店里的左轮格子替换成胶带。
 * 其它商品和特殊购买行为继续由 Wathe 默认杀手商店提供，避免复制整张商店后与本体脱节。</p>
 */
public final class MuzzlerShopHandler {
    private MuzzlerShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.MUZZLER) {
            return;
        }

        replaceItem(entries, WatheItems.REVOLVER, new ShopEntry(
                ModItems.TAPE.getDefaultStack(),
                MuzzlerConstants.TAPE_PRICE,
                ShopEntry.Type.WEAPON
        ));
    }

    private static void replaceItem(@NotNull List<ShopEntry> entries, @NotNull Item item, @NotNull ShopEntry replacement) {
        int index = indexOfItem(entries, item);
        if (index >= 0) {
            entries.set(index, replacement);
        }
    }

    private static int indexOfItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).stack().isOf(item)) {
                return i;
            }
        }
        return -1;
    }
}
