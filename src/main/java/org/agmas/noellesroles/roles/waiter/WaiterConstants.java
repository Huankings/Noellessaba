package org.agmas.noellesroles.roles.waiter;

import dev.doctor4t.wathe.game.GameConstants;

import java.awt.Color;

/**
 * 服务员职业的数值集中表。
 *
 * <p>需求里除职业 RGB 外，其余数值尽量常量化，所以交互距离、持续时间、商店价格、
 * 药水时长和托盘同类数量限制都统一放在这里。后续调平衡时优先改这个类，避免数值散落在各个 handler 中。</p>
 */
public final class WaiterConstants {
    // 职业色按需求固定为 RGB(225, 170, 40)，actionbar、透视和职业注册都复用这一份。
    public static final int ROLE_COLOR = new Color(225, 170, 40).getRGB();

    // Wathe/Noelles 的普通任务金币是 50；服务员帮别人完成任务时给 25。
    public static final int TASK_INCOME = 50;
    public static final int HELP_BONUS = 25;
    public static final int SERVE_OTHER_INCOME = HELP_BONUS;

    // 完成心情任务后的服务员被动透视、睡袋失明和服务交互距离。
    public static final int VISIBLE_TICKS = GameConstants.getInTicks(0, 4);
    public static final int BLINDNESS_TICKS = GameConstants.getInTicks(0, 4);
    public static final float INTERACTION_RANGE = 3.0F;

    // 服务员从托盘拿同类物品时，背包和主手中该物品合计最多保留 2 份。
    public static final int MAX_TRAY_ITEM_COUNT = 2;

    // 服务员商店价格，顺序由 WaiterShopHandler#getShopEntries 决定。
    public static final int RANDOM_DRINK_PRICE = 100;
    public static final int RANDOM_FOOD_PRICE = 100;
    public static final int RANDOM_POTION_PRICE = 100;
    public static final int BAR_STOOL_PRICE = 100;
    public static final int FISHING_ROD_PRICE = 100;
    public static final int MUSIC_DISC_PRICE = 100;
    public static final int CAMPFIRE_PRICE = 100;
    public static final int SMOKER_PRICE = 100;
    public static final int SLEEPING_BAG_PRICE = 100;
    public static final int BOOK_PRICE = 100;

    // 商店钓鱼竿固定为 1 耐久、饵钓 5；购买交付时会再写入附魔，避免注册期缺少玩家 registry。
    public static final int FISHING_ROD_MAX_DAMAGE = 1;
    public static final int FISHING_ROD_LURE_LEVEL = 5;

    // 随机药水池和具体效果参数。
    public static final int RANDOM_POTION_VARIANTS = 5;
    public static final int STRONG_EFFECT_AMPLIFIER = 1;
    public static final int BASE_EFFECT_AMPLIFIER = 0;
    // 商店随机药水图标使用再生药水色，实际购买后会随机生成具体药水。
    public static final int REGENERATION_POTION_COLOR = 0xCD5CAB;
    public static final int INSTANT_EFFECT_DURATION_TICKS = 1;
    public static final int REGENERATION_DURATION_TICKS = GameConstants.getInTicks(0, 22);
    public static final int STRENGTH_DURATION_TICKS = GameConstants.getInTicks(1, 30);
    public static final int WATER_BREATHING_DURATION_TICKS = GameConstants.getInTicks(3, 0);
    public static final int FIRE_RESISTANCE_DURATION_TICKS = GameConstants.getInTicks(3, 0);

    // 毒药物品经服务员递予后沿用 Wathe 的毒药系统；如果目标已有毒，则提前一段随机时间结算。
    public static final int POISON_STACK_ACCELERATION_MIN_TICKS = 100;
    public static final int POISON_STACK_ACCELERATION_MAX_TICKS = 300;

    private WaiterConstants() {
    }
}
