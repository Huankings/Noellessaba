package org.agmas.noellesroles.modifiers.lovers;

/**
 * 恋人词条的固定数值。
 *
 * <p>这些值来自 StupidExpress 原 config 的默认值。
 * 按本仓库约定，除用户明确要求保留动态配置的项目外，迁移后的玩法数值集中放到常量类。</p>
 */
public final class LoversConstants {
    public static final int COLOR = 0xf38aff;
    public static final int MAX_RANDOM_PAIRS = 1;
    public static final boolean KNOW_IMMEDIATELY = true;
    public static final boolean WIN_WITH_KILLERS = false;
    public static final boolean WIN_WITH_CIVILIANS = true;
    public static final boolean GLOW_TO_EACH_OTHER = true;

    private LoversConstants() {
    }
}
