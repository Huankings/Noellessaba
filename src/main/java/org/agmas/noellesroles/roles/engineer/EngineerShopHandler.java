package org.agmas.noellesroles.roles.engineer;

import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 工程师商店条目。
 * 保持原本 noellesroles 的价格与道具配置，只把商店定义集中出来。
 */
public final class EngineerShopHandler {

    private EngineerShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 工具箱：修门、解卡门。
        entries.add(new ShopEntry(ModItems.TOOLBOX.getDefaultStack(), 65, ShopEntry.Type.TOOL));
        // 捕捉装置：用于范围控制与生成报告。
        entries.add(new ShopEntry(ModItems.CAPTURE_DEVICE.getDefaultStack(), 150, ShopEntry.Type.TOOL));
        // 电力恢复系统：恢复停电并给予夜视。
        entries.add(new ShopEntry(ModItems.POWER_RESTORATION.getDefaultStack(), 225, ShopEntry.Type.TOOL));

        return entries;
    }
}
