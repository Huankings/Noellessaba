package org.agmas.noellesroles.roles.recaller;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 回溯者相关常量。
 *
 * <p>把“保存点冷却 / 传送花费 / 传送后冷却”统一集中到这里，
 * 后续要调数值时就不需要再到 Ability、HUD 和语言文本里分别找一遍。</p>
 */
public final class RecallerConstants {

    /**
     * 第二次按下能力键时，回溯到已保存位置所需的金币。
     */
    public static final int TELEPORT_COST = 100;

    /**
     * 第一次保存位置后的冷却时间：10 秒。
     */
    public static final int SAVE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 10);

    /**
     * 成功回溯后的冷却时间：30 秒。
     */
    public static final int TELEPORT_COOLDOWN_TICKS = GameConstants.getInTicks(0, 30);

    private RecallerConstants() {
    }
}
