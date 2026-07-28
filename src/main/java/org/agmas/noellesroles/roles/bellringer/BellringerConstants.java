package org.agmas.noellesroles.roles.bellringer;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 敲钟人职业常量。
 *
 * <p>kinssaba 原实现通过配置读取价格、冷却和减少时间；
 * 迁入 NoellesRoles 后按本仓库约定固化到职业常量里，避免继续依赖 kinssaba 配置。</p>
 */
public final class BellringerConstants {
    public static final int ROLE_COLOR = 0x66B2FF;
    public static final int ABILITY_PRICE = 100;
    public static final int REDUCE_SECONDS = 60;
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(0, 0);

    private BellringerConstants() {
    }
}
