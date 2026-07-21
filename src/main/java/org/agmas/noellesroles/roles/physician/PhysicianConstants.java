package org.agmas.noellesroles.roles.physician;

import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.game.GameConstants;

/**
 * 医师职业的所有玩法数值集中在这里。
 */
public final class PhysicianConstants {
    public static final int ROLE_COLOR = 0xFFE5CC;
    public static final int MAX_SPRINT_TIME_NUMERATOR = 3;
    public static final int MAX_SPRINT_TIME_DENOMINATOR = 2;
    public static final int MEDICAL_KIT_COOLDOWN_TICKS = GameConstants.getInTicks(1, 0);
    public static final int PILL_COOLDOWN_TICKS = GameConstants.getInTicks(3, 0);
    public static final int PILL_ARMOR_AMOUNT = 1;
    public static final int PILL_SHOP_PRICE = 150;
    public static final int MEDICAL_KIT_REWARD = 50;
    public static final int MEDICAL_KIT_TASKMASTER_REWARD = 75;
    public static final int TASK_INCOME = 50;
    public static final float BODY_HUD_RANGE = 2.0F;

    private PhysicianConstants() {
    }

    public static int getMaxSprintTimeTicks() {
        /*
         * kinssaba 原医师冲刺时间是普通平民的 1.5 倍。
         * 这里保留“跟随 Wathe 平民基础值”的语义，同时把倍率本身放到职业常量，后续调参只改本类。
         */
        return WatheRoles.CIVILIAN.getMaxSprintTime() * MAX_SPRINT_TIME_NUMERATOR / MAX_SPRINT_TIME_DENOMINATOR;
    }
}
