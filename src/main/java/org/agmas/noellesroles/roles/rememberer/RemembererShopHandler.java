package org.agmas.noellesroles.roles.rememberer;

import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 追忆者商店。
 *
 * <p>当前只卖狙击枪子弹，保持玩法聚焦。</p>
 */
public final class RemembererShopHandler {

    private RemembererShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();
        entries.add(new ShopEntry(
                ModItems.SNIPER_RIFLE_BULLET.getDefaultStack(),
                RemembererConstants.SNIPER_BULLET_PRICE,
                ShopEntry.Type.WEAPON
        ));
        return entries;
    }
}
