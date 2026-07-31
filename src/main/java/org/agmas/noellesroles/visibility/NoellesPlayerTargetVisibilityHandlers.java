package org.agmas.noellesroles.visibility;

/**
 * 玩家实体隐藏 / 不可选中规则的预留入口。
 *
 * <p>当前 NoellesRoles 还没有职业需要隐藏活玩家，因此这里暂时不注册任何规则。
 * 后续新增职业时，请把具体逻辑写在对应 {@code roles/<role>/} 或词条包里，
 * 然后只在这个入口补一行 {@code XxxTargetVisibilityHandler.init()}。</p>
 */
public final class NoellesPlayerTargetVisibilityHandlers {
    private static boolean initialized = false;

    private NoellesPlayerTargetVisibilityHandlers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
    }
}
