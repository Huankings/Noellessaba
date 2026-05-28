package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 变形怪相关常量。
 *
 * <p>这里统一收口变形持续时间与冷却时间，
 * 方便你后续做平衡时只改一处即可。</p>
 */
public final class MorphlingConstants {

    /**
     * 变形持续时间：1 分 10 秒。
     */
    public static final int MORPH_DURATION_TICKS = GameConstants.getInTicks(1, 10);

    /**
     * 变形结束后冷却：5 秒。
     */
    public static final int MORPH_COOLDOWN_TICKS = GameConstants.getInTicks(0, 5);

    private MorphlingConstants() {
    }
}
