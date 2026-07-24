package org.agmas.noellesroles.roles.controller;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.NoellesRolesShops;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 附体师商店修改器。
 *
 * <p>附体师只需要对默认杀手商店做很少的局部调整：
 * 移除毒药瓶/蝎子，调整疯魔模式和裹尸袋价格。其他条目，包括购买行为和显示顺序，
 * 都继续继承 Wathe 默认杀手商店，避免把整套商店复制一份后和本体后续改动脱节。</p>
 */
public final class ControllerShopHandler {

    private ControllerShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.CONTROLLER) {
            return;
        }

        /*
         * 只移除附体师禁售的两种毒物；其他默认杀手商品，包括左轮、刀、手雷等都保留。
         * 这里操作的是 Wathe 为当前玩家复制出来的临时列表，不会污染全局默认商店。
         */
        removeItem(entries, WatheItems.POISON_VIAL);
        removeItem(entries, WatheItems.SCORPION);

        /*
         * 疯魔模式是“购买即触发”的特殊商品，不能用普通 ShopEntry 替换，
         * 否则会变成把 PSYCHO_MODE 物品塞进背包。这里用 action 保留原本的即时启动逻辑。
         */
        replaceItem(entries, WatheItems.PSYCHO_MODE, ShopEntry.action(
                WatheItems.PSYCHO_MODE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.PSYCHO_MODE, 350) + 50,
                ShopEntry.Type.WEAPON,
                PlayerShopComponent::usePsychoMode
        ));

        // 裹尸袋保留原本的普通购买行为，只把价格改成 Wathe 当前默认价的一半。
        replaceItem(entries, WatheItems.BODY_BAG, new ShopEntry(
                WatheItems.BODY_BAG.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.BODY_BAG, 70) / 2,
                ShopEntry.Type.TOOL
        ));
    }

    private static void removeItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        entries.removeIf(entry -> entry.stack().isOf(item));
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
