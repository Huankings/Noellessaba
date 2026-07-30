package org.agmas.noellesroles.client.appearance;

/**
 * NoellesRoles 所有 Wathe 外观 / 准心名字接入点的优先级集中表。
 *
 * <p>同一职业的皮肤和名字必须使用同一优先级，避免玩家看见“皮肤已经伪装、
 * 但准星名字还停在原玩家”的短暂穿帮。</p>
 */
public final class NoellesAppearancePriorities {
    /**
     * 灵术师是本地客户端视角规则：出窍/附体时它描述的是“自己如何看世界”，
     * 所以需要压过所有全局伪装和普通主动变形。
     */
    public static final int SPIRITUALIST = 2000;

    /**
     * 时间狭缝是强视角隔离：玩家在狭缝内看到的其它存活玩家都应像自己。
     * 它必须高于灵术师、召集者、变形怪等普通伪装，避免狭缝期间仍泄露真实外观。
     */
    public static final int TIMEKEEPER_RIFT = 3000;

    /**
     * 召集者会在全体活人身上强制套尸体皮肤，必须高于普通主动变形。
     */
    public static final int CONVENER = 1000;

    /**
     * 疯狂观察来自心情导致的视觉错乱；它低于召集者等强制全员伪装，
     * 但高于 Morphling/Controller/Coroner 这类普通主动变形。
     */
    public static final int INSANE_OBSERVER = 900;

    /**
     * 普通主动变形的基准优先级，皮肤和准心名字一同覆盖。
     */
    public static final int ACTIVE_DISGUISE = 100;

    /**
     * 变形试剂触发后的外观伪装。
     *
     * <p>它低于变形怪原本的主动变形，保证变形怪自己同时处于两套伪装时，
     * 背包主动变形仍然按原 NoellesRoles 规则显示。</p>
     */
    public static final int MORPH_REAGENT_DISGUISE = 90;

    /**
     * 双重人格副人格默认显示为主人格。
     * 这是低优先级兜底：如果召集者、变形怪、控制者等主动伪装存在，应优先显示那些更明确的效果。
     */
    public static final int DUAL_PERSONALITY = -100;

    /**
     * 休眠人格视角不应通过准心 HUD 读取身份信息。
     * 这个规则只隐藏 HUD，不改名字，因此单独给较高优先级。
     */
    public static final int DUAL_PERSONALITY_DORMANT_VISIBILITY = 1100;

    /**
     * 共用名字规则只处理隐身等横向词条，略低于主动伪装。
     */
    public static final int SHARED_NAME_RULES = 95;

    private NoellesAppearancePriorities() {
    }
}
