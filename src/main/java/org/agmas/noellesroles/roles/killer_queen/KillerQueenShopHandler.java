package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;

import java.util.List;

/** 杀手皇后的固定专属商店。 */
public final class KillerQueenShopHandler {
    private static final List<ShopEntry> ENTRIES = List.of(
            new ShopEntry(ModItems.ACID_SOLUTION.getDefaultStack(), KillerQueenConstants.ACID_SOLUTION_PRICE_COINS, ShopEntry.Type.POISON),
            new ShopEntry(ModItems.DELUSION_VIAL.getDefaultStack(), KillerQueenConstants.DELUSION_VIAL_PRICE_COINS, ShopEntry.Type.POISON),
            ShopEntry.action(ModItems.PSYCHO_KILLER_QUEEN.getDefaultStack(), KillerQueenConstants.PSYCHO_PRICE_COINS,
                    ShopEntry.Type.WEAPON, KillerQueenPsychoHandler::start),
            new ShopEntry(WatheItems.LOCKPICK.getDefaultStack(), KillerQueenConstants.LOCKPICK_PRICE_COINS, ShopEntry.Type.TOOL),
            new ShopEntry(WatheItems.FIRECRACKER.getDefaultStack(), KillerQueenConstants.FIRECRACKER_PRICE_COINS, ShopEntry.Type.TOOL),
            new ShopEntry(new ItemStack(WatheItems.NOTE, KillerQueenConstants.NOTE_STACK_COUNT), KillerQueenConstants.NOTE_PRICE_COINS, ShopEntry.Type.TOOL)
    );

    private KillerQueenShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        return ENTRIES;
    }
}
