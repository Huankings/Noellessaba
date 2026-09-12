package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;

/**
 * 执照恶棍职业常量。
 *
 * <p>这些数值来自 kinssaba 原实现。原 mod 里有 YACL config，
 * 搬到 NoellesRoles 后按本仓库约定改成职业自己的常量，避免再引入一套配置同步组件。</p>
 */
public final class LicensedVillainConstants {
    public static final int ROLE_COLOR = 0x404040;
    public static final int MIN_PLAYER_COUNT = 10;
    public static final int REVOLVER_PRICE = 100;
    public static final int TASK_INCOME_COINS = 50;

    /** 执照恶棍时刻期间，Wathe 左轮的固定冷却为 2 秒。 */
    public static final int MOMENT_REVOLVER_COOLDOWN_TICKS = GameConstants.getInTicks(0, 2);

    /** 执照恶棍时刻全场环境音的最大音量。 */
    public static final float MUSIC_BASE_VOLUME = 0.7F;

    /** 环境音保持原始音高播放。 */
    public static final float MUSIC_PITCH = 1.0F;

    /** 每客户端 tick 调整 0.05 音量，约 1 秒完成淡入或淡出。 */
    public static final float MUSIC_FADE_STEP = 0.05F;

    private LicensedVillainConstants() {
    }

    public static int getMaxSprintTimeTicks() {
        /*
         * kinssaba 原源码使用 WatheRoles.CIVILIAN.getMaxSprintTime() * 3 / 2。
         * 这里继续按源码行为跟随 Wathe 平民冲刺时间，而不是把旧 guidebook 的秒数硬编码进来。
         */
        return WatheRoles.CIVILIAN.getMaxSprintTime() * 3 / 2;
    }
}
