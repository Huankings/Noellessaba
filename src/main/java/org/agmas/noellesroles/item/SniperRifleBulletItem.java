package org.agmas.noellesroles.item;

import net.minecraft.item.Item;

/**
 * 狙击枪子弹。
 *
 * <p>真正的装填逻辑写在 {@link SniperRifleItem#onClicked}，
 * 这样就能复刻“把子弹拿到光标上，再右键点枪本体完成装填”的交互手感。</p>
 */
public class SniperRifleBulletItem extends Item {

    public SniperRifleBulletItem(Settings settings) {
        super(settings);
    }
}
