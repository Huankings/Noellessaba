package org.agmas.noellesroles.roles.Noisemaker;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * 大嗓门专属商店。
 *
 * <p>整体定位仍然是“制造混乱、误导他人”：
 * 鞭炮负责制造声响干扰，假刀和假左轮负责近距离与远距离的心理威慑，
 * 新加入的假手雷则负责把这种误导扩展到投掷爆炸场景。
 */
public final class NoisemakerShopHandler {

    private NoisemakerShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 鞭炮继续作为低价干扰道具，方便大嗓门频繁制造声响。
        entries.add(new ShopEntry(WatheItems.FIRECRACKER.getDefaultStack(), 25, ShopEntry.Type.TOOL));

        // 假武器系列负责“看起来很危险，但实际上不具备真实击杀能力”。
        entries.add(new ShopEntry(ModItems.FAKE_KNIFE.getDefaultStack(), 75, ShopEntry.Type.POISON));
        entries.add(new ShopEntry(ModItems.FAKE_REVOLVER.getDefaultStack(), 125, ShopEntry.Type.POISON));

        // 假手雷保留真手雷的蓄力投掷、爆炸音效与粒子表现，
        // 但在爆炸结算时会被 noellesroles 的 Mixin 拦截，不会杀死人。
        entries.add(new ShopEntry(ModItems.FAKE_GRENADE.getDefaultStack(), 225, ShopEntry.Type.WEAPON));

        return entries;
    }
}
