package org.agmas.noellesroles.roles.cleaner;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 清道夫职业常量。
 *
 * <p>原 kinssaba 实现把价格、收益和冷却放在 config 里；
 * 迁入 NoellesRoles 后按本仓库约定固化为职业常量，避免继续依赖 kinssaba 配置。</p>
 */
public final class CleanerConstants {
    public static final int ROLE_COLOR = 0x16582C;
    public static final int ABILITY_PRICE = 150;
    public static final int DISSOLVE_REWARD_COINS = 50;
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(2, 0);
    public static final int SULFURIC_ACID_BARREL_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);

    private CleanerConstants() {
    }
}
