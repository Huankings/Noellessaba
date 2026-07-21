package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.game.GameConstants;

/**
 * 厨师职业的所有玩法数值集中在这里。
 *
 * <p>kinssaba 原先有一部分数值写在 config，一部分直接写在物品和数据包里。
 * 搬进 NoellesRoles 后统一放到职业常量，后续调平衡时不需要再到处翻实现类。</p>
 */
public final class CookConstants {
    public static final int ROLE_COLOR = 0xCCFF99;
    public static final int EAT_MARK_TICKS = GameConstants.getInTicks(0, 40);
    public static final int PAN_COOLDOWN_TICKS = GameConstants.getInTicks(0, 45);
    public static final int PAN_STUN_TICKS = GameConstants.getInTicks(0, 5);
    public static final int PAN_MIN_USE_TICKS = 10;
    public static final int PAN_CLIENT_SEND_GRACE_TICKS = 5;
    public static final int PAN_MAX_USE_TICKS = 100;
    public static final int PAN_MAX_USE_TICKS_FOR_HIT = PAN_MAX_USE_TICKS - PAN_CLIENT_SEND_GRACE_TICKS - 1;
    public static final float PAN_TARGET_RANGE = 3.0F;
    public static final int PAN_SHOP_PRICE = 170;
    public static final int COOKED_FOOD_SHOP_PRICE = 25;
    public static final int TASK_INCOME = 50;

    private CookConstants() {
    }
}
