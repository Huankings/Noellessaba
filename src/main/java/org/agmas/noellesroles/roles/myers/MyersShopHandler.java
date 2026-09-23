package org.agmas.noellesroles.roles.myers;

import dev.doctor4t.wathe.api.economy.CurrencyAmount;
import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.api.shop.ShopPrice;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import java.util.List;

/** 迈尔斯默认杀手商店的局部替换。 */
public final class MyersShopHandler {
    private MyersShopHandler() {
    }

    public static void modifyShop(ShopContext context, List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.MYERS) return;

        remove(entries, WatheItems.GRENADE);
        remove(entries, WatheItems.POISON_VIAL);
        remove(entries, WatheItems.SCORPION);
        remove(entries, WatheItems.CROWBAR);
        remove(entries, WatheItems.BLACKOUT);
        remove(entries, WatheItems.NOTE);

        replace(entries, WatheItems.REVOLVER, new ShopEntry(
                WatheItems.REVOLVER.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, 250) + 150,
                ShopEntry.Type.WEAPON
        ));

        replace(entries, WatheItems.PSYCHO_MODE, ShopEntry.action(
                ModItems.EVIL_POSSESS.getDefaultStack(),
                ShopPrice.money(0),
                ShopEntry.Type.WEAPON,
                MyersPsychoHandler::start
        ));
        int index = indexOf(entries, ModItems.EVIL_POSSESS);
        if (index >= 0) {
            int price = MyersPlayerComponent.KEY.get(context.player()).getInitialParticipantCount()
                    * MyersConstants.EVIL_POSSESS_PRICE_PER_PLAYER;
            entries.set(index, ShopEntry.action(
                    ModItems.EVIL_POSSESS.getDefaultStack(),
                    ShopPrice.allOf(CurrencyAmount.of(MyersConstants.MALICE_CURRENCY_ID, price)),
                    ShopEntry.Type.WEAPON,
                    MyersPsychoHandler::start
            ));
        }
    }

    private static void remove(List<ShopEntry> entries, Item item) {
        entries.removeIf(entry -> entry.stack().isOf(item));
    }

    private static int indexOf(List<ShopEntry> entries, Item item) {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).stack().isOf(item)) return i;
        return -1;
    }

    private static void replace(List<ShopEntry> entries, Item original, ShopEntry replacement) {
        int index = indexOf(entries, original);
        if (index >= 0) entries.set(index, replacement);
    }
}
