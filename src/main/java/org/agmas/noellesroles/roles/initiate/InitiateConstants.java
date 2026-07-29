package org.agmas.noellesroles.roles.initiate;

/**
 * 初学者职业常量。
 *
 * <p>从 StupidExpress 迁移过来的配置值都收口在这里，避免玩法数值散落在注册和事件逻辑里。</p>
 */
public final class InitiateConstants {
    public static final int ROLE_COLOR = 0xffd154;
    public static final int TASK_INCOME_COINS = 50;
    public static final int KNIFE_PRICE_FALLBACK = 100;
    public static final int KNIFE_PRICE_BONUS = 100;
    public static final int MIN_KILLER_SLOTS_FOR_PAIR = 2;

    /**
     * 杀手开启本能时是否能直接识别初学者。
     *
     * <p>初学者仍然属于普通中立，不会加入杀手侧中立池；这个开关只控制杀手本能是否把它
     * 作为特殊目标显示为初学者职业色。关闭时客户端会阻止通用中立绿色兜底继续显示它，
     * 这样开关语义就是“杀手本能看不见初学者”。</p>
     */
    public static final boolean GLOWS_TO_KILLER_INSTINCT = true;

    private InitiateConstants() {
    }
}
