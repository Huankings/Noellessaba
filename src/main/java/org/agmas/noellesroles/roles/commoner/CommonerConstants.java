package org.agmas.noellesroles.roles.commoner;

/** 平民职业的固定玩法常量。 */
public final class CommonerConstants {
    /** 平民职业色。 */
    public static final int ROLE_COLOR = 0x36E51B;
    /** 默认最少参局人数：达到 13 人后才允许进行平民概率判定。 */
    public static final int DEFAULT_MIN_PLAYER_SPAWN = 13;
    /** 默认进入平民随机职业池的概率。 */
    public static final double DEFAULT_SPAWN_CHANCE = 0.50D;
    /** 平民随机职业最大生成数量。 */
    public static final int MAX_ROLE_COUNT = 1;
    /** 平民死亡后转化倒计时，单位为秒。 */
    public static final int REVIVAL_DELAY_SECONDS = 45;

    private CommonerConstants() {
    }
}
