package org.agmas.noellesroles.roles.detective;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 侦探职业常量。
 *
 * <p>kinssaba 原实现的配置项在迁入 NoellesRoles 后集中到这里，
 * 服务端能力、客户端 HUD 和准心提示都共用同一份数值。</p>
 */
public final class DetectiveConstants {
    public static final int ROLE_COLOR = 0xFFFFCC;
    public static final int ABILITY_PRICE = 125;
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);
    public static final float TARGET_RANGE = 2.0f;

    private DetectiveConstants() {
    }
}
