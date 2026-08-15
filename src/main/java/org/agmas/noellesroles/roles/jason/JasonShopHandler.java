package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.shop.ShopContext;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 杰森对 Wathe 默认杀手商店的局部改写。
 *
 * <p>杰森仍保留 Wathe 默认杀手商店中没有被需求明确替换的条目，因此这里使用
 * {@code ShopModifier} 而不是接管一整套职业商店。这样 Wathe 后续新增的通用杀手商品
 * 也会自然继承，同时只修改毒物、停电、左轮、手雷和疯魔模式四个指定位置。</p>
 */
public final class JasonShopHandler {
    private static final Item[] RANDOM_THROWING_WEAPONS = {
            ModItems.THROWING_BLOOD_AXE,
            ModItems.THROWING_MACHETE,
            ModItems.TOMAHAWK,
            ModItems.THROWING_TOYS_AXE
    };

    private JasonShopHandler() {
    }

    public static void modifyShop(@NotNull ShopContext context, @NotNull List<ShopEntry> entries) {
        if (context.role() != NoellesRoleRegistry.JASON) {
            return;
        }

        // 杰森没有毒药瓶和蝎子加上停电这3个商品位，直接从默认商店移除。
        removeItem(entries, WatheItems.POISON_VIAL);
        removeItem(entries, WatheItems.SCORPION);
        removeItem(entries, WatheItems.BLACKOUT);

        /*
         * 左轮格子显示为“随机投掷武器”图标；真正购买时随机交付四把普通杰森投掷武器之一，
         * 不会把纯商店图标放进玩家背包。
         */
        replaceItem(entries, WatheItems.REVOLVER, ShopEntry.action(
                ModItems.RANDOM_THROWING_WEAPON.getDefaultStack(),
                JasonConstants.RANDOM_THROWING_WEAPON_PRICE,
                ShopEntry.Type.WEAPON,
                JasonShopHandler::giveRandomThrowingWeapon
        ));

        // 手雷格子固定替换为投掷油桶。
        replaceItem(entries, WatheItems.GRENADE, new ShopEntry(
                ModItems.THROWING_JERRY_CAN.getDefaultStack(),
                JasonConstants.THROWING_JERRY_CAN_PRICE,
                ShopEntry.Type.WEAPON
        ));

        /*
         * 杰森模式是即时执行的商店动作：Wathe 默认疯魔价格加 50 金币，
         * 真正的持续时间、护盾、飞镐授予和结束回收由 JasonPsychoHandler 的 profile 管理。
         */
        replaceItem(entries, WatheItems.PSYCHO_MODE, ShopEntry.action(
                ModItems.PSYCHO_JASON.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.PSYCHO_MODE, 350) + JasonConstants.PSYCHO_PRICE_BONUS,
                ShopEntry.Type.WEAPON,
                JasonPsychoHandler::startJasonMode
        ));
    }

    private static boolean giveRandomThrowingWeapon(@NotNull PlayerEntity player) {
        Item item = RANDOM_THROWING_WEAPONS[player.getRandom().nextInt(RANDOM_THROWING_WEAPONS.length)];
        return player.giveItemStack(new ItemStack(item));
    }

    private static void removeItem(@NotNull List<ShopEntry> entries, @NotNull Item item) {
        entries.removeIf(entry -> entry.stack().isOf(item));
    }

    private static void replaceItem(@NotNull List<ShopEntry> entries, @NotNull Item original, @NotNull ShopEntry replacement) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).stack().isOf(original)) {
                entries.set(index, replacement);
                return;
            }
        }
    }
}
