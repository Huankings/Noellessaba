package org.agmas.noellesroles.bootstrap;

/**
 * Wathe RoundEndFactionApi 的分类注册入口，当前有意保留为空。
 *
 * <p>未来按 roles/&lt;role&gt;/&lt;RoleName&gt;RoundEndHandler 或 modifiers/&lt;modifier&gt;/*RoundEndHandler
 * 拆分玩家结算迁移规则，此处只调用各 handler 的 init()。规则在服务端生成结算快照前读取词条等条件，
 * 不应只判断玩家当前存活，否则带词条的死者会被错误移回原阵营。
 * 迁移同时改变普通阵营个人胜负；独立胜利/共胜仍由 Wathe 的 CustomVictory 与额外赢家列表仲裁。
 * 本次不创建善良/内鬼等具体词条，空入口不会改变任何现有结算结果。</p>
 */
public final class NoellesRolesRoundEndBootstrap {
    private NoellesRolesRoundEndBootstrap() {
    }

    public static void init() {
        // 未来在这里调用各职业/词条自己的 RoundEndHandler.init()。
    }
}
