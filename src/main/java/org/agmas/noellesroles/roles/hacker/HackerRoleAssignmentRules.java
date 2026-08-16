package org.agmas.noellesroles.roles.hacker;

import org.agmas.harpymodloader.api.assignment.RoleAssignmentApi;
import org.agmas.harpymodloader.api.assignment.RoleAssignmentPhase;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 黑客相关的 Harpy 职业分配规则。
 *
 * <p>旧实现通过 mixin 拦截 Harpy 私有分配函数来阻止 Hacker 与 Mimic 同局生成。
 * 现在改为注册 Harpy 公开 API：规则仍限定在平民替换阶段，因为 Hacker 是中立位、
 * Mimic 是平民位，二者都在这一阶段完成替换。</p>
 */
public final class HackerRoleAssignmentRules {
    private static boolean initialized = false;

    private HackerRoleAssignmentRules() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        RoleAssignmentApi.registerMutualExclusion(
                NoellesRolesCore.id("hacker_mimic_assignment_exclusion"),
                RoleAssignmentPhase.CIVILIAN_REPLACEMENT,
                () -> !HackerConstants.GENERATE_WITH_MIMIC,
                NoellesRoleRegistry.HACKER,
                NoellesRoleRegistry.MIMIC
        );
    }
}
