package org.agmas.noellesroles.roles.dreamer;

/**
 * 梦者职业常量。
 *
 * <p>从 kinssaba 迁移时，原本散落在配置文件里的玩法数值收束到这里；
 * 这样 NoellesRoles 不需要反向依赖 kinssaba 的 config，也方便后续单独调整梦者。</p>
 */
public final class DreamerConstants {
    public static final int ROLE_COLOR = 0xE5CCFF;
    public static final int INITIAL_DREAM_IMPRINT_COUNT = 2;
    public static final int BECOME_KILLER_REWARD_COINS = 100;
    public static final int REQUIRED_PLAYER_DIVISOR = 4;

    private DreamerConstants() {
    }
}
