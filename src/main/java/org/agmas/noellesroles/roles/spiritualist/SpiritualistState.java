package org.agmas.noellesroles.roles.spiritualist;

/**
 * 灵术师的主状态枚举。
 *
 * <p>这里明确只保留三种互斥状态：
 * 1. 正常；
 * 2. 灵魂出窍；
 * 3. 灵魂附身。
 *
 * <p>后续所有能力判定、HUD、客户端视角切换、交互封锁，
 * 都统一以这一个状态源为准，避免出现“出窍中还能附身”或
 * “附身中又进入出窍”的重叠 bug。</p>
 */
public enum SpiritualistState {
    NORMAL("normal"),
    PROJECTING("projecting"),
    POSSESSING("possessing");

    private final String serializedName;

    SpiritualistState(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public static SpiritualistState fromSerializedName(String rawName) {
        for (SpiritualistState state : values()) {
            if (state.serializedName.equals(rawName)) {
                return state;
            }
        }
        return NORMAL;
    }
}
