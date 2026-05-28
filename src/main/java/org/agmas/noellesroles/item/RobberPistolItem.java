package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.item.RevolverItem;

/**
 * 强盗手枪。
 * 这里直接继承 Wathe 左轮，确保客户端开火手感、射线瞄准和动画完全一致。
 * 真正的“击杀后保留 / 掉普通左轮 / 消失”逻辑在 GunShootPayload 的服务端 mixin 中接管。
 */
public class RobberPistolItem extends RevolverItem {

    public RobberPistolItem(Settings settings) {
        super(settings);
    }
}
