package org.agmas.noellesroles.roles.kidnapper;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 绑匪杀手商店修改器。
 */
public final class KidnapperShopHandler {
    private KidnapperShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != Noellesroles.KIDNAPPER) {
            return;
        }

        // 绑匪旧商店不出售撬棍，其余默认杀手商品继续保留。
        removeItem(entries, WatheItems.CROWBAR);

        /*
         * 迷药价格使用 kinssaba config 当前值 75，并插在疯魔模式之后。
         * 如果 Wathe 默认商店以后调整其它条目，绑匪仍能自然继承。
         */
        insertAfterItem(entries, WatheItems.PSYCHO_MODE, new ShopEntry(
                ModItems.KNOCKOUT_DRUG.getDefaultStack(),
                KidnapperConstants.KNOCKOUT_DRUG_PRICE,
                ShopEntry.Type.POISON
        ));
    }

    private static void removeItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        entries.removeIf(entry -> entry.stack().isOf(item));
    }

    private static void insertAfterItem(@NotNull List<ShopEntry> entries, @NotNull Item item, @NotNull ShopEntry entry) {
        int index = indexOfItem(entries, item);
        entries.add(index >= 0 ? index + 1 : entries.size(), entry);
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
