package org.agmas.noellesroles.roles.prophet;

import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 先知商店。
 *
 * <p>当前只出售水晶球，后续如果你要扩展更多占卜类道具，
 * 可以直接继续往这里追加。</p>
 */
public final class ProphetShopHandler {

    private ProphetShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();
        entries.add(new ShopEntry(
                ModItems.CRYSTAL_BALL.getDefaultStack(),
                ProphetConstants.CRYSTAL_BALL_PRICE,
                ShopEntry.Type.TOOL
        ));
        return entries;
    }
}
