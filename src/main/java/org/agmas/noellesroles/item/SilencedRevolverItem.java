package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.item.RevolverItem;

/**
 * 无声左轮。
 *
 * <p>客户端仍直接复用 Wathe 左轮的射线、后坐和手部火花粒子，
 * 真正去掉枪声、接管掉枪逻辑的是服务端 GunShootPayload mixin。</p>
 */
public class SilencedRevolverItem extends RevolverItem {

    public SilencedRevolverItem(Settings settings) {
        super(settings);
    }
}
