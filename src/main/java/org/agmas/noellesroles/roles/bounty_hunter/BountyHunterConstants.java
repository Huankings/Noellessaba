package org.agmas.noellesroles.roles.bounty_hunter;

import dev.doctor4t.wathe.game.GameConstants;

import java.awt.Color;

/**
 * 赏金猎人的全部玩法数值。
 *
 * <p>职业颜色本身也放在这里，方便注册、HUD、高亮和目标标记都引用同一份颜色。</p>
 */
public final class BountyHunterConstants {
    public static final int ROLE_COLOR = new Color(230, 230, 50).getRGB();

    public static final int MAX_ROLE_COUNT = 1;
    public static final int BOUNTY_REWARD_COINS = 50;

    public static final int START_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);
    public static final int BOUNTY_PISTOL_TARGET_COOLDOWN_TICKS = GameConstants.getInTicks(0, 15);
    public static final int BOUNTY_PISTOL_FAILED_COOLDOWN_TICKS = GameConstants.getInTicks(0, 35);
    public static final int BOUNTY_DERRINGER_COOLDOWN_TICKS = GameConstants.getInTicks(0, 1);
    public static final int BOUNTY_MODE_DURATION_TICKS = GameConstants.getInTicks(0, 30);
    public static final int BOUNTY_MODE_COOLDOWN_TICKS = GameConstants.getInTicks(3, 40);
    /*
     * 赏金模式复用 Wathe 的疯魔状态来获得皮肤、心情图标和死亡保护链，
     * 但护盾层数是赏金猎人自己的平衡参数，不再跟随 Wathe 原版疯魔模式的数值。
     */
    public static final int BOUNTY_MODE_SHIELD_LAYERS = 0;

    public static final float BOUNTY_PISTOL_RANGE_BLOCKS = 15.0F;
    public static final float BOUNTY_DERRINGER_RANGE_BLOCKS = 7.0F;

    private BountyHunterConstants() {
    }
}
