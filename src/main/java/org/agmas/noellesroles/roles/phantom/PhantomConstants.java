package org.agmas.noellesroles.roles.phantom;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 幻灵技能常量。
 *
 * <p>用户后续会频繁测试平衡，因此把持续时间和冷却时间抽成常量，
 * 这样比把数字散落在 Ability 里更容易统一调整。</p>
 */
public final class PhantomConstants {

    /**
     * 幻灵隐身持续时间：35 秒。
     */
    public static final int INVISIBILITY_DURATION_TICKS = GameConstants.getInTicks(0, 35);

    /**
     * 幻灵技能冷却：1 分 30 秒。
     */
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(1, 30);

    private PhantomConstants() {
    }
}
