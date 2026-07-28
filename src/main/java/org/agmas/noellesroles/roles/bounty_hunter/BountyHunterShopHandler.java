package org.agmas.noellesroles.roles.bounty_hunter;

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
 * 赏金猎人的默认杀手商店修改器。
 *
 * <p>赏金猎人仍然是杀手，所以这里不重建整套商店，只在 Wathe 默认杀手商店上做局部删除/替换。
 * 这样以后 Wathe 默认杀手商店如果增加通用商品，赏金猎人能自动继承，只有明确禁售的条目会被过滤掉。</p>
 */
public final class BountyHunterShopHandler {
    private BountyHunterShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.BOUNTY_HUNTER) {
            return;
        }

        removeItem(entries, WatheItems.KNIFE);
        removeItem(entries, WatheItems.GRENADE);
        removeItem(entries, WatheItems.POISON_VIAL);
        removeItem(entries, WatheItems.SCORPION);
        removeItem(entries, WatheItems.CROWBAR);

        replaceItem(entries, WatheItems.REVOLVER, new ShopEntry(
                ModItems.BOUNTY_PISTOL.getDefaultStack(),
                Math.max(0, NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, 250) - 100),
                ShopEntry.Type.WEAPON
        ));
        /*
         * 赏金模式和工程师的电力恢复系统一样，是“购买即触发”的商店图标。
         *
         * 这里不能用普通 new ShopEntry(...)：赏金猎人商店是 ShopModifier，
         * 购买时会走 Wathe 默认 provider 的 ShopEntry#onBuy，而普通 ShopEntry#onBuy
         * 只会把 stack 塞进快捷栏，于是玩家拿到的就是 bounty_mode 图标。
         *
         * 改成 ShopEntry.action 后，商店仍显示同一个图标和价格，但购买成功判定会直接调用
         * 赏金猎人组件启动模式；扣钱、音效、回放仍交给 Wathe 的 PlayerShopComponent 统一处理。
         */
        replaceItem(entries, WatheItems.PSYCHO_MODE, ShopEntry.action(
                ModItems.BOUNTY_MODE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.PSYCHO_MODE, 350)+50,
                ShopEntry.Type.WEAPON,
                player -> BountyHunterPlayerComponent.KEY.get(player).tryStartBountyMode()
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
