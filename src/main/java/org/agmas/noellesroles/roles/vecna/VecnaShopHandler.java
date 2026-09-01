package org.agmas.noellesroles.roles.vecna;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import java.util.List;

/** 维克那默认杀手商店的局部修改。 */
public final class VecnaShopHandler {
    private VecnaShopHandler() {}
    public static void modifyShop(ShopContext context, List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.VECNA) return;
        remove(entries, WatheItems.GRENADE);
        remove(entries, WatheItems.POISON_VIAL);
        remove(entries, WatheItems.SCORPION);
        int psychoIndex = indexOf(entries, WatheItems.PSYCHO_MODE);
        replace(entries, WatheItems.PSYCHO_MODE, ShopEntry.action(ModItems.PSYCHO_VECNA.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.PSYCHO_MODE, 350) + VecnaConstants.PSYCHO_PRICE_BONUS,
                ShopEntry.Type.WEAPON, VecnaPsychoHandler::start));
        if (psychoIndex < 0) psychoIndex = entries.size() - 1;
        entries.add(Math.min(psychoIndex + 1, entries.size()), ShopEntry.action(ModItems.VECNA_ADDTIME.getDefaultStack(),
                VecnaConstants.ADD_TIME_PRICE, ShopEntry.Type.TOOL,
                player -> { GameTimeComponent.KEY.get(player.getWorld()).addTime(VecnaConstants.ADD_TIME_TICKS); return true; }));
    }
    private static void remove(List<ShopEntry> entries, Item item) { entries.removeIf(e -> e.stack().isOf(item)); }
    private static int indexOf(List<ShopEntry> entries, Item item) { for (int i=0;i<entries.size();i++) if (entries.get(i).stack().isOf(item)) return i; return -1; }
    private static void replace(List<ShopEntry> entries, Item item, ShopEntry replacement) { int i=indexOf(entries,item); if(i>=0) entries.set(i,replacement); }
}
