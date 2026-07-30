package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 变形怪杀手商店局部改写。
 *
 * <p>这里不替换整张商店，只在 Wathe 默认杀手商店上把毒药瓶原位替换成变形试剂，并移除蝎子。
 * 购买扣钱、音效、失败提示和购买回放继续交给 Wathe ShopApi/PlayerShopComponent，
 * 避免复制一套商店结算流程。</p>
 */
public final class MorphlingShopHandler {
    private MorphlingShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.MORPHLING) {
            return;
        }

        ShopEntry reagent = new ShopEntry(
                ModItems.MORPH_REAGENT.getDefaultStack(),
                MorphlingConstants.MORPH_REAGENT_PRICE,
                ShopEntry.Type.POISON
        );

        /*
         * 变形试剂在玩法定位上替代原毒药瓶，所以直接占用毒药瓶原本的位置。
         * 这样不会因为后续 Wathe 默认杀手商店调整顺序，导致变形怪商店的其它商品被重新排列。
         */
        int poisonIndex = indexOfItem(entries, WatheItems.POISON_VIAL);
        if (poisonIndex >= 0) {
            entries.set(poisonIndex, reagent);
        } else {
            /*
             * 极端情况下其它 ShopModifier 已经提前移除了毒药瓶，此时仍给变形怪保底提供试剂，
             * 但不再尝试猜测疯魔/手雷等位置，避免把“原位替换”逻辑扩散成整张商店重排。
             */
            entries.add(reagent);
        }

        removeItem(entries, WatheItems.SCORPION);
    }

    private static void removeItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        entries.removeIf(entry -> entry.stack().isOf(item));
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
