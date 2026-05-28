package org.agmas.noellesroles.roles.stalker;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.NoellesRolesShops;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StalkerShopHandler {

    private StalkerShopHandler() {
    }

    /**
     * 按潜行者当前阶段生成商店条目。
     *
     * <p>角色注册在统一的 {@code NoellesRolesShopBootstrap} 中完成，
     * 这里专注于“潜行者在不同阶段卖什么”。</p>
     */
    public static @NotNull List<ShopEntry> getShopEntries(@NotNull PlayerEntity player) {
        List<ShopEntry> entries = new ArrayList<>();
        StalkerPlayerComponent component = StalkerPlayerComponent.KEY.get(player);
        int phase = component.phase;

        // 阶段一：只能购买开锁器。
        entries.add(new ShopEntry(
                WatheItems.LOCKPICK.getDefaultStack(),
                NoellesRolesShops.getItemPrice(WatheItems.LOCKPICK, 50),
                ShopEntry.Type.TOOL
        ));

        // 阶段二：在一阶段基础上解锁匕首和左轮。
        if (phase >= 2) {
            entries.add(new ShopEntry(
                    WatheItems.KNIFE.getDefaultStack(),
                    NoellesRolesShops.getItemPrice(WatheItems.KNIFE, 100),
                    ShopEntry.Type.WEAPON
            ));
            entries.add(new ShopEntry(
                    WatheItems.REVOLVER.getDefaultStack(),
                    NoellesRolesShops.getItemPrice(WatheItems.REVOLVER, 250) + 100,
                    ShopEntry.Type.WEAPON
            ));
        }

        // 阶段三：在二阶段基础上解锁手雷。
        if (phase >= 3) {
            entries.add(new ShopEntry(
                    WatheItems.GRENADE.getDefaultStack(),
                    NoellesRolesShops.getItemPrice(WatheItems.GRENADE, 300) * 2,
                    ShopEntry.Type.WEAPON
            ));
        }

        return entries;
    }
}
