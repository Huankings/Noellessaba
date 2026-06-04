package org.agmas.noellesroles.roles.bomber;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesShops;

import java.util.ArrayList;
import java.util.List;

/**
 * 炸弹客的专属商店列表。
 * 这里不直接修改 Wathe 原始商店，而是像 Stalker 一样，用单独的条目列表覆盖原商店内容。
 */
public class BomberShopHandler {

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 定时炸弹是炸弹客的核心商品，按需求固定售价 75 金币
        entries.add(new ShopEntry(
                ModItems.TIMED_BOMB.getDefaultStack(),
                75,
                ShopEntry.Type.WEAPON
        ));

        // 手雷在 Wathe 原价基础上减 65 金币
        entries.add(new ShopEntry(
                WatheItems.GRENADE.getDefaultStack(),
                Math.max(0, NoellesRolesShops.getItemPrice(WatheItems.GRENADE, 300) - 65),
                ShopEntry.Type.WEAPON
        ));
        // 无声手雷,在原手雷基础上增加15金币
        entries.add(new ShopEntry(
                ModItems.SILENT_GRENADE.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.GRENADE, 300) + 15,
                ShopEntry.Type.WEAPON
        ));

        // 其余工具价格直接沿用 Wathe 原价，方便后续统一跟随基底调整
        entries.add(new ShopEntry(
                WatheItems.LOCKPICK.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.LOCKPICK, 50),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                WatheItems.BODY_BAG.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.BODY_BAG, 70),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                WatheItems.BLACKOUT.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.BLACKOUT, 200),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                WatheItems.FIRECRACKER.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.FIRECRACKER, 10),
                ShopEntry.Type.TOOL
        ));
        entries.add(new ShopEntry(
                new ItemStack(WatheItems.NOTE, 4),
                NoellesRolesShops.getItemPrice(WatheItems.NOTE, 10),
                ShopEntry.Type.TOOL
        ));

        return entries;
    }

//    /**
//     * 这里原本用于限制炸弹客重复购买定时炸弹。
//     * 目前按需求暂时停用，所以先整体注释保留，方便后续需要时直接恢复。
//     */
//    public static boolean canBuyTimedBomb(PlayerEntity player) {
//        if (hasTimedBombItem(player)) {
//            return false;
//        }
//        return !BomberPlayerComponent.hasBombInCirculation(player);
//    }
//
//    private static boolean hasTimedBombItem(PlayerEntity player) {
//        for (int i = 0; i < player.getInventory().size(); i++) {
//            if (player.getInventory().getStack(i).isOf(ModItems.TIMED_BOMB)) {
//                return true;
//            }
//        }
//        return false;
//    }
}
