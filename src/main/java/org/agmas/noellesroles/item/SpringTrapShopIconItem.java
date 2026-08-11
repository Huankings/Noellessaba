package org.agmas.noellesroles.item;

import net.minecraft.item.Item;

/**
 * 弹簧陷阱商店图标。
 *
 * <p>购买时通过 ShopEntry.action 立即触发，不应该作为普通物品进入背包。</p>
 */
public class SpringTrapShopIconItem extends Item {
    public SpringTrapShopIconItem(Settings settings) {
        super(settings);
    }
}
