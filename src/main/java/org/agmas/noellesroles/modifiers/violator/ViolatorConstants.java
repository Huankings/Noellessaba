package org.agmas.noellesroles.modifiers.violator;

/**
 * 违禁者词条的集中数值。
 *
 * <p>原 kinssaba 用服务端配置默认关闭自动生成；迁移到 NoellesRoles 后，
 * 这里改成常量开关，方便后续只改源码常量就能调整默认池行为。</p>
 */
public final class ViolatorConstants {
    public static final int COLOR = 0x660000;
    public static final boolean DEFAULT_DISABLE_AUTO_GENERATION = true;

    private ViolatorConstants() {
    }
}
