package org.agmas.noellesroles.bootstrap;

/**
 * Wathe PlayerMoodTypeApi 的分类注册入口，当前有意保留为空。
 *
 * <p>这里只编排各职业/词条 handler 的 init()，不集中编写职业判断。后续真实心情与假心情转换规则
 * 放在 roles/&lt;role&gt;/&lt;RoleName&gt;MoodTypeHandler 或 modifiers/&lt;modifier&gt;/*MoodTypeHandler。
 * handler 在通用初始化注册 resolver，判断条件必须来自两端都已同步的职业/词条/组件状态。
 * 本次只公开能力，不创建示例中的善良、内鬼词条，也不注册任何改变现有职业心情类型的规则。</p>
 */
public final class NoellesRolesMoodTypeBootstrap {
    private NoellesRolesMoodTypeBootstrap() {
    }

    public static void init() {
        // 未来在这里调用各职业/词条自己的 MoodTypeHandler.init()。
    }
}
