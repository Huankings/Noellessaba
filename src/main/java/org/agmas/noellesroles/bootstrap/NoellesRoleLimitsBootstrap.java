package org.agmas.noellesroles.bootstrap;

import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.modifiers.lovers.LoversConstants;
import org.agmas.noellesroles.registry.NoellesRoleIds;

/**
 * NoellesRoles 的 Harpy 生成上限初始化。
 *
 * <p>这里只负责开服时的静态上限。人数相关的动态上限仍由服务端 tick 事件实时刷新，
 * 这样“固定限制”和“随玩家数变化的限制”不会继续混在入口类里。</p>
 */
public final class NoellesRoleLimitsBootstrap {
    private NoellesRoleLimitsBootstrap() {
    }

    public static void initStaticLimits() {
        Harpymodloader.setRoleMaximum(NoellesRoleIds.CONDUCTOR_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.EXECUTIONER_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.VULTURE_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.JESTER_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.DREAMER_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.HACKER_ID, 0);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.BETTER_VIGILANTE_ID, 0);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.STARSTRUCK_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.MUZZLER_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.AMNESIAC_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.ARSONIST_ID, 1);
        Harpymodloader.setRoleMaximum(NoellesRoleIds.CONVENER_ID, 1);
        Harpymodloader.MODIFIER_MAX.put(NoellesRoleIds.LOVERS_ID, LoversConstants.MAX_RANDOM_PAIRS);
        /*
         * 双重人格的随机上限不是固定值：
         * 开局 Harpy 分配词条前，会按本局参局人数和 Noelles 配置动态刷新。
         * 这里先设为 0，避免服务器刚启动但还未进入分配流程时被误放进随机池。
         */
        Harpymodloader.MODIFIER_MAX.put(NoellesRoleIds.DUAL_PERSONALITY_ID, 0);
    }
}
