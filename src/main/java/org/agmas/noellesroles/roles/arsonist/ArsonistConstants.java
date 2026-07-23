package org.agmas.noellesroles.roles.arsonist;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 纵火犯的固定玩法数值。
 */
public final class ArsonistConstants {
    public static final int ROLE_COLOR = 0xfc9526;

    /**
     * 对应 StupidExpress 配置 arsonistKeepsGameGoing 的默认值。
     *
     * <p>false 表示纵火犯只保留道具即时胜利逻辑，不额外拖住普通阵营结算。</p>
     */
    public static final boolean KEEPS_GAME_GOING = true;

    public static final double DOUSED_REQUIRED_RATIO = 0.3D;
    public static final int LARGE_GAME_PLAYER_THRESHOLD = 15;
    public static final int LARGE_GAME_DOUSE_COOLDOWN_TICKS = GameConstants.getInTicks(0, 20);
    public static final int BASE_DOUSE_COOLDOWN_SECONDS = 45;
    public static final double DOUSE_COOLDOWN_REDUCTION_PER_PLAYER_SECONDS = 5.0D / 3.0D;

    private ArsonistConstants() {
    }

    public static int getDouseCooldownTicks(int alivePlayerCount) {
        if (alivePlayerCount > LARGE_GAME_PLAYER_THRESHOLD) {
            return LARGE_GAME_DOUSE_COOLDOWN_TICKS;
        }
        double seconds = BASE_DOUSE_COOLDOWN_SECONDS
                - DOUSE_COOLDOWN_REDUCTION_PER_PLAYER_SECONDS * alivePlayerCount;
        return Math.max(0, (int) (seconds * 20.0D));
    }

    public static int getRequiredDousedCount(int alivePlayerCount) {
        return (int) (alivePlayerCount * DOUSED_REQUIRED_RATIO);
    }
}
