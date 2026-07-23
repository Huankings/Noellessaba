package org.agmas.noellesroles.roles.amnesiac;

/**
 * 失忆患者的固定玩法数值。
 *
 * <p>这些值来自 StupidExpress 的服务端配置；迁入 NoellesRoles 后改成常量，
 * 避免运行时继续依赖另一个扩展的 config 结构。</p>
 */
public final class AmnesiacConstants {
    public static final int ROLE_COLOR = 0x9baae8;

    /**
     * 失忆患者可以直接看到可交互尸体的高亮。
     */
    public static final boolean BODIES_GLOW_TO_AMNESIAC = true;

    /**
     * 杀手本能看到失忆患者时显示失忆患者职业色。
     *
     * <p>这不是把失忆患者加入杀手中立池，而是一条独立的本能颜色规则：
     * 失忆患者仍然是普通中立，只是允许杀手在本能开启时识别这个特殊目标。</p>
     */
    public static final boolean AMNESIAC_GLOWS_DIFFERENTLY = true;

    private AmnesiacConstants() {
    }
}
