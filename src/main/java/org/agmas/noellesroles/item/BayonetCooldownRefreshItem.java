package org.agmas.noellesroles.item;

import net.minecraft.item.Item;

/**
 * 刺刀冷却刷新图标。
 *
 * <p>这个物品本身没有主动使用逻辑，
 * 只作为商店中的“即时生效图标”存在，
 * 真正的刷新动作会在购买成功瞬间由服务端直接执行。</p>
 */
public class BayonetCooldownRefreshItem extends Item {

    public BayonetCooldownRefreshItem(Settings settings) {
        super(settings);
    }
}
