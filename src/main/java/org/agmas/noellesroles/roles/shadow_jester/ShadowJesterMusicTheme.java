package org.agmas.noellesroles.roles.shadow_jester;

/**
 * 谢幕时刻的环境音主题。
 *
 * <p>第四阶段只保存“哪一边先死完”这个稳定结果，客户端再根据结果选择实际 SoundEvent。
 * 这样时停者回溯恢复世界组件后，客户端可以重新开始正确的循环音乐。</p>
 */
public enum ShadowJesterMusicTheme {
    NONE("none"),
    KING("king"),
    QUEEN("queen");

    private final String serialized;

    ShadowJesterMusicTheme(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return this.serialized;
    }

    public static ShadowJesterMusicTheme fromSerialized(String serialized) {
        for (ShadowJesterMusicTheme theme : values()) {
            if (theme.serialized.equals(serialized)) {
                return theme;
            }
        }
        return NONE;
    }
}
