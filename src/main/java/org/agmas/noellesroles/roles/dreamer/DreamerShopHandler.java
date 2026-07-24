package org.agmas.noellesroles.roles.dreamer;

import org.agmas.noellesroles.registry.NoellesFramingShopEntries;

import dev.doctor4t.wathe.util.ShopEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 梦者商店。
 *
 * <p>梦者保留 kinssaba 的“复用 Noelles 伪装商店”设计。
 * 由于现在职业就在 NoellesRoles 内，直接返回共享列表即可，不再需要反射读取。</p>
 */
public final class DreamerShopHandler {
    private DreamerShopHandler() {
    }

    public static @NotNull List<ShopEntry> getShopEntries() {
        return NoellesFramingShopEntries.FRAMING_ROLES_SHOP;
    }
}
