package org.agmas.noellesroles.roles.shadow_jester;

import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.victory.NoellesRolesVictoryUtil;

import java.util.List;
import java.util.UUID;

/**
 * 影子小丑第三阶段后的独立胜利规则。
 *
 * <p>一旦双方缔结誓言，他们就不再依附好人、杀手或其它独立中立结算。
 * 场上只剩这对影子小丑时独立胜利；否则只要普通阵营尝试结算，就继续保活拦截。</p>
 */
public final class ShadowJesterVictoryRule {
    private static final int PRIORITY = 95;
    private static boolean initialized = false;

    private ShadowJesterVictoryRule() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        VictoryApi.registerRule(NoellesRolesCore.id("victory/shadow_jester"), PRIORITY, context -> {
            ShadowJesterComponent component = ShadowJesterComponent.KEY.get(context.world());
            if (!component.hasPair()) {
                return VictoryApi.VictoryResult.pass();
            }
            UUID first = component.first();
            UUID second = component.second();
            if (first == null || second == null
                    || !component.getPhase(first).atLeast(ShadowJesterPhase.VOW_BOUND)
                    || !component.getPhase(second).atLeast(ShadowJesterPhase.VOW_BOUND)) {
                return VictoryApi.VictoryResult.pass();
            }
            if (component.areBothPairMembersConfirmedOrPendingDeath()) {
                /*
                 * 需求只要求“双方确实死亡/待补死”时放开普通结算。
                 * 这里不能用 alivePlayers 缺人作为失败条件，否则管理员把其中一名小丑切 creative
                 * 测试时，Wathe 会把他从 alivePlayers 里移除，普通阵营就会抢先结算。
                 */
                return VictoryApi.VictoryResult.pass();
            }
            if (!context.gameWorld().isRole(first, NoellesRoleRegistry.SHADOW_JESTER)
                    || !context.gameWorld().isRole(second, NoellesRoleRegistry.SHADOW_JESTER)) {
                return VictoryApi.VictoryResult.pass();
            }

            /*
             * 独立胜利只允许组件里登记的这一对影子小丑获胜。
             * 调试指令可能临时造成额外 SHADOW_JESTER 玩家出现，但这些人不属于当前
             * ShadowJesterComponent pair，既不能被写入赢家列表，也不能让“全体影子小丑”
             * 这种宽泛条件误触发共同胜利。
             */
            if (context.alivePlayers().stream().noneMatch(player -> !component.contains(player.getUuid()))) {
                return NoellesRolesVictoryUtil.customWinUuids(
                        NoellesRoleRegistry.SHADOW_JESTER.identifier(),
                        NoellesRoleRegistry.SHADOW_JESTER.color(),
                        List.of(first, second)
                );
            }

            if (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS
                    || context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS) {
                return VictoryApi.VictoryResult.keepRunning();
            }
            return VictoryApi.VictoryResult.pass();
        });
    }
}
