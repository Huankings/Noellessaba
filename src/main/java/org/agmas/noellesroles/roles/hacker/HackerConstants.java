package org.agmas.noellesroles.roles.hacker;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 黑客职业常量。
 *
 * <p>kinssaba 的黑客原先从配置读取生成人数、破解时间、奖励和商店价格。
 * 迁入 NoellesRoles 后按本仓库约定全部改成常量，避免保留跨模组配置依赖。</p>
 */
public final class HackerConstants {
    public static final int ROLE_COLOR = 0x808080;
    public static final int PLAYER_LIMIT = 10;
    public static final int HACKING_TIME_TICKS = GameConstants.getInTicks(0, 20);
    public static final int HACK_REWARD_COINS = 100;
    public static final boolean HAS_SHOP = true;
    public static final boolean GENERATE_WITH_MIMIC = false;

    public static final int REFRESH_WEAPON_COOLDOWN_PRICE = 300;
    public static final int REFRESH_ABILITY_COOLDOWN_PRICE = 400;
    public static final int REFRESH_POTION_EFFECT_PRICE = 200;

    public static final int REFRESH_WEAPON_COOLDOWN_TICKS = GameConstants.getInTicks(3, 0);
    public static final int REFRESH_ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(5, 0);
    public static final int REFRESH_POTION_EFFECT_TICKS = GameConstants.getInTicks(3, 0);

    private HackerConstants() {
    }
}
