package org.agmas.noellesroles.bootstrap;

import dev.doctor4t.wathe.api.blackout.BlackoutApi;
import dev.doctor4t.wathe.api.blackout.BlackoutEffectResult;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * NoellesRoles 对 Wathe 停电机制的接入总入口。
 *
 * <p>这里只有“按 Noelles 共享分组注册停电药水规则”的分发逻辑。
 * 后续某个具体职业需要改停电时长、禁用药水或获得特殊效果时，
 * 请把实现拆到 {@code roles/<role>/<RoleName>BlackoutHandler}，
 * 再从这个 bootstrap 调用对应 handler 的 init()，不要把所有职业逻辑堆在这里。</p>
 */
public final class NoellesRolesBlackoutBootstrap {
    private NoellesRolesBlackoutBootstrap() {
    }

    public static void init() {
        /*
         * Wathe 默认只认识 Faction.NEUTRAL 这一大类，并会让中立默认吃到平民/义警同款失明。
         * NoellesRoles 需要更细：
         * 1. 杀手侧中立在停电中视作杀手侧协同，获得夜视；
         * 2. 独立中立仍按普通乘客视角处理，获得失明。
         *
         * 这里使用同一个 API 分别按分组注册，后续职业只需要维护 NoellesRoleGroups 或追加自己的 handler。
         */
        BlackoutApi.registerEffectRule(NoellesRolesCore.id("killer_sided_neutral_blackout_effect"), 100, context ->
                context.role() != null && NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(context.role())
                        ? BlackoutEffectResult.nightVision()
                        : BlackoutEffectResult.pass()
        );
        BlackoutApi.registerEffectRule(NoellesRolesCore.id("independent_neutral_blackout_effect"), 100, context ->
                context.role() != null && NoellesRoleGroups.INDEPENDENT_NEUTRALS.contains(context.role())
                        ? BlackoutEffectResult.blindness()
                        : BlackoutEffectResult.pass()
        );
    }
}
