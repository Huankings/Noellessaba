package org.agmas.noellesroles.roles.commoner;

/** 平民职业的固定玩法常量。 */
public final class CommonerConstants {
    /** 平民职业色。 */
    public static final int ROLE_COLOR = 0x36E51B;
    /** 只有当前人数大于 12 人时，平民才进入随机分配池。 */
    public static final int RANDOM_POOL_MIN_PLAYER_COUNT = 12;
    /** 平民随机职业最大生成数量。 */
    public static final int MAX_ROLE_COUNT = 1;
    /** 平民死亡后转化倒计时，单位为秒。 */
    public static final int REVIVAL_DELAY_SECONDS = 45;

    private CommonerConstants() {
    }
}
