package org.agmas.noellesroles.roles.necromancer;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 死灵法师全部玩法数值。
 *
 * <p>这些值来自 StupidExpress 的实际代码实现。旧 config 中“死灵法师是否有商店”
 * 搬运后固定为常量，符合 NoellesRoles 当前新增职业的配置风格。</p>
 */
public final class NecromancerConstants {
    private NecromancerConstants() {
    }

    public static final int ROLE_COLOR = 0x9457ff;
    public static final int REVIVE_COOLDOWN_TICKS = GameConstants.getInTicks(3, 0);
    public static final int REVIVE_COOLDOWN_SECONDS = 180;
    public static final int REVIVED_BALANCE = 200;
    public static final boolean HAS_KILLER_SHOP = true;

    /** 给 StarryExpress 绿皮书读取用，避免说明文字和 NoellesRoles 当前常量漂移。 */
    public static int guidebookReviveCooldownSeconds() {
        return REVIVE_COOLDOWN_SECONDS;
    }

    /** 同上，绿皮书中文/英文说明按分钟展示会更自然。 */
    public static int guidebookReviveCooldownMinutes() {
        return REVIVE_COOLDOWN_SECONDS / 60;
    }
}
