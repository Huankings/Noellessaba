package org.agmas.noellesroles.roles.Noisemaker;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 大嗓门相关常量。
 *
 * <p>这次把技能冷却和发光持续时间抽出来，
 * 后续你如果想调测试节奏，只需要改这里，不用再去包里到处找硬编码。</p>
 */
public final class NoisemakerConstants {

    /**
     * 大嗓门点亮技能冷却：75 秒。
     */
    public static final int ABILITY_COOLDOWN_TICKS = GameConstants.getInTicks(1, 15);

    /**
     * 发光持续时间：30 秒。
     */
    public static final int GLOW_DURATION_TICKS = GameConstants.getInTicks(0, 30);

    private NoisemakerConstants() {
    }
}
