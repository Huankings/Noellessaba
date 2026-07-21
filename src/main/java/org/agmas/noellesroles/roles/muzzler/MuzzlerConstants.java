package org.agmas.noellesroles.roles.muzzler;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 静语者全部玩法数值。
 *
 * <p>这些默认值来自 StarryExpress 1.3.2 的 Muzzler config。
 * 搬运到 NoellesRoles 后统一变成职业常量，供职业逻辑和 StarryExpress 绿皮书共同读取。</p>
 */
public final class MuzzlerConstants {
    private MuzzlerConstants() {
    }

    public static final int ROLE_COLOR = 0x370387;
    public static final int TAPE_PRICE = 65;

    public static final int TAPE_COOLDOWN_SECONDS = 5;
    public static final int TAPE_COOLDOWN_TICKS = GameConstants.getInTicks(0, TAPE_COOLDOWN_SECONDS);

    public static final int SUFFOCATION_SECONDS = 1;
    public static final int SUFFOCATION_TICKS = GameConstants.getInTicks(0, 1);

    public static final int TAPE_TEAR_CHECK_COUNT = 5;
    public static final float TAPE_TEAR_MOOD_CHANGE = 1.0F;
    public static final boolean KILL_IF_CHECKED_AT_ZERO = true;

    public static final int DISPLAY_SILENCED_TIP_DELAY_SECONDS = 120;
    public static final int DISPLAY_SILENCED_TIP_DELAY_TICKS = GameConstants.getInTicks(0, 30);

    /**
     * 给 StarryExpress 绿皮书读取用的运行时入口。
     * 直接跨模组引用 public static final int 会被 Java 编译器内联，方法调用能让绿皮书运行时读取当前 NoellesRoles jar。
     */
    public static int guidebookSuffocationSeconds() {
        return SUFFOCATION_SECONDS;
    }

    /** 同上，保持绿皮书“撕胶带次数”说明与 NoellesRoles 当前逻辑一致。 */
    public static int guidebookTapeTearCheckCount() {
        return TAPE_TEAR_CHECK_COUNT;
    }
}
