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
     * 疯狂观察来自心情导致的视觉错乱；它低于召集者等强制全员伪装，
     * 但高于 Morphling/Controller/Coroner 这类普通主动变形。
     */
    public static final int INSANE_OBSERVER = 900;

    /**
     * 普通主动变形的基准优先级，皮肤和准心名字一同覆盖。
     */
    public static final int ACTIVE_DISGUISE = 100;

    /**
     * 共用名字规则只处理隐身等横向词条，略低于主动伪装。
     */
    public static final int SHARED_NAME_RULES = 95;

    private NoellesAppearancePriorities() {
    }
}
