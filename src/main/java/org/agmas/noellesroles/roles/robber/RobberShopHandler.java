package org.agmas.noellesroles.roles.robber;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 强盗商店修改器。
 *
 * <p>强盗并不需要完全脱离 Wathe 默认杀手商店；它只是对默认杀手商店做一组有顺序的改动。
 * 因此这里作为 {@code ShopApi.registerShopModifier(...)} 的处理器使用：先拿到 Wathe 已经构建好的
 * 默认杀手商品列表，再按强盗规则局部移除、插入、改价和换位。</p>
 */
public class RobberShopHandler {

    private RobberShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.ROBBER) {
            return;
        }

        /*
         * 第一步：删掉强盗不应该购买的默认杀手商品。
         * 因为 entries 是 Wathe 传入的本次临时列表，这里只会影响当前玩家本次看到的商店，
         * 不会改写 GameConstants.SHOP_ENTRIES 这个全局默认常量。
         */
        removeItem(entries, WatheItems.REVOLVER);
        removeItem(entries, WatheItems.PSYCHO_MODE);
        removeItem(entries, WatheItems.POISON_VIAL);
        removeItem(entries, WatheItems.SCORPION);

        /*
         * 第二步：手雷仍然保留默认杀手商店的位置和类型，但价格比 Wathe 默认价贵 15 金币。
         * 价格通过 NoellesRolesShops.getItemPrice 读取 Wathe 当前默认价格，避免两边价格写死后漂移。
         */
        replaceItem(entries, WatheItems.GRENADE, new ShopEntry(
                WatheItems.GRENADE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.GRENADE, 300) + 15,
                ShopEntry.Type.WEAPON
        ));

        /*
         * 第三步：把强盗专属武器插入到 KNIFE 后面。
         * 由于 REVOLVER 已经被移除，最终顺序会变成：
         * KNIFE -> ROBBER_PISTOL -> THROWING_AXE -> GRENADE。
         */
        int knifeIndex = indexOfItem(entries, WatheItems.KNIFE);
        int insertIndex = knifeIndex >= 0 ? knifeIndex + 1 : 0;
        entries.add(insertIndex++, new ShopEntry(
                ModItems.ROBBER_PISTOL.getDefaultStack(),
                Math.max(0, NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, 250) - 120),
                ShopEntry.Type.WEAPON
        ));
        entries.add(insertIndex, new ShopEntry(
                ModItems.THROWING_AXE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100) + 55,
                ShopEntry.Type.WEAPON
        ));

        /*
         * 第四步：其他商品继续沿用默认杀手商店，只额外交换 FIRECRACKER 和 BLACKOUT 的显示位置。
         * 这里交换的是列表元素本身，所以 BLACKOUT 原有的“购买即停电”特殊 onBuy 逻辑会被完整保留。
         */
        swapItems(entries, WatheItems.FIRECRACKER, WatheItems.BLACKOUT);
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

    private static void swapItems(@NotNull List<ShopEntry> entries, @NotNull Item first, @NotNull Item second) {
        int firstIndex = indexOfItem(entries, first);
        int secondIndex = indexOfItem(entries, second);
        if (firstIndex < 0 || secondIndex < 0 || firstIndex == secondIndex) {
            return;
        }

        ShopEntry firstEntry = entries.get(firstIndex);
        entries.set(firstIndex, entries.get(secondIndex));
        entries.set(secondIndex, firstEntry);
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
