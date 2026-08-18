package org.agmas.noellesroles.bootstrap;

import org.agmas.noellesroles.roles.spiritualist.SpiritualistMoodTaskHandler;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterTaskHandler;

/**
 * NoellesRoles 的 Wathe 心情任务 API 接入分发器。
 *
 * <p>这个类只负责聚合调用，不承载具体职业逻辑。
 * 后续如果某个职业或词条要新增自己的心情任务、任务点透视或任务完成拦截，
 * 应继续放到对应 {@code roles/<role>/} 或 {@code modifiers/<modifier>/} 包内，
 * 再在这里补一行 {@code XxxMoodTaskHandler.init()}。</p>
 *
 * <p>当前 NoellesRoles 暂不注册新的自定义心情任务；这里先接入从旧 mixin 迁出的任务完成规则。</p>
 */
public final class NoellesRolesMoodTaskBootstrap {
    private static boolean initialized = false;

    private NoellesRolesMoodTaskBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        SpiritualistMoodTaskHandler.init();
        ShadowJesterTaskHandler.init();
    }
}
