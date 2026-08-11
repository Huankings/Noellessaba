package org.agmas.noellesroles.roles.spring_trap;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 弹簧陷阱默认杀手商店修改器。
 *
 * <p>只对 Wathe 默认杀手商店做局部替换：后续 Wathe 新增的通用杀手商品仍会自然继承。</p>
 */
public final class SpringTrapShopHandler {
    private SpringTrapShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.SPRING_TRAP) {
            return;
        }

        removeItem(entries, WatheItems.POISON_VIAL);
        removeItem(entries, WatheItems.SCORPION);

        replaceItem(entries, WatheItems.KNIFE, new ShopEntry(
                ModItems.BLOOD_AXE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100),
                ShopEntry.Type.WEAPON
        ));
        replaceItem(entries, WatheItems.REVOLVER, new ShopEntry(
                ModItems.THROWING_SPEED_AXE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, 250),
                ShopEntry.Type.WEAPON
        ));
        replaceItem(entries, WatheItems.GRENADE, new ShopEntry(
                ModItems.THROWING_BOMB_AXE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.GRENADE, 300) + SpringTrapConstants.THROWING_BOMB_AXE_GRENADE_PRICE_BONUS,
                ShopEntry.Type.WEAPON
        ));

        int psychoIndex = replaceItem(entries, WatheItems.PSYCHO_MODE, ShopEntry.action(
                ModItems.SPRING_TRAP.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.PSYCHO_MODE, 350) + SpringTrapConstants.SPRING_TRAP_SHOP_PRICE_BONUS,
                ShopEntry.Type.WEAPON,
                SpringTrapPsychoHandler::startSpringTrapMode
        ));
        entries.add((psychoIndex >= 0 ? psychoIndex + 1 : entries.size()), ShopEntry.action(
                ModItems.SPRING_TRAP_ADDTIME.getDefaultStack(),
                SpringTrapConstants.SPRING_TRAP_ADDTIME_PRICE,
                ShopEntry.Type.TOOL,
                SpringTrapPsychoHandler::extendSpringTrapMode
        ));
    }

    private static void removeItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        entries.removeIf(entry -> entry.stack().isOf(item));
    }

    private static int replaceItem(@NotNull List<ShopEntry> entries, @NotNull Item item, @NotNull ShopEntry replacement) {
        int index = indexOfItem(entries, item);
        if (index >= 0) {
            entries.set(index, replacement);
        }
        return index;
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
