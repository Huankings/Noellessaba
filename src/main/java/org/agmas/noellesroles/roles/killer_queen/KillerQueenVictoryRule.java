package org.agmas.noellesroles.roles.killer_queen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleGroups;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 杀手皇后“所有存活好人和独立中立均被标记”的即时独胜规则。 */
public final class KillerQueenVictoryRule {
    private KillerQueenVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(NoellesRolesCore.id("victory/killer_queen"), VictoryApi.DEFAULT_PRIORITY + KillerQueenConstants.VICTORY_PRIORITY_BONUS, context -> {
            /*
             * 不直接把 VictoryApi 的 alivePlayers 当成最终目标集合：时间狭缝会给死者临时存活授权，
             * 但这些玩家不能成为“还需要标记的目标”。KillerQueenRules 同时排除时间狭缝，
             * 因而剩余真实存活的合法玩家全被标记后可以立即胜利。
             */
            List<ServerPlayerEntity> eligible = context.alivePlayers().stream()
                    .filter(KillerQueenRules::isLivingGoodOrIndependent).toList();
            if (eligible.isEmpty()) return VictoryApi.VictoryResult.pass();
            KillerQueenWorldComponent state = KillerQueenWorldComponent.KEY.get(context.world());
            for (ServerPlayerEntity queen : context.alivePlayers().stream()
                    .filter(player -> context.gameWorld().isRole(player, NoellesRoleRegistry.KILLER_QUEEN)
                            && GameFunctions.isPlayerAliveAndSurvival(player)
                            && !TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()).toList()) {
                if (eligible.stream().allMatch(target -> state.isMarkedBy(queen.getUuid(), target.getUuid()))) {
                    Set<UUID> winners = new LinkedHashSet<>();
                    winners.add(queen.getUuid());
                    for (var role : dev.doctor4t.wathe.api.WatheRoles.ROLES) {
                        if (role.getFaction() == Faction.KILLER || NoellesRoleGroups.KILLER_SIDED_NEUTRALS.contains(role)) {
                            winners.addAll(context.gameWorld().getAllWithRole(role));
                        }
                    }
                    winners.addAll(state.getRecruitedPlayers());
                    return VictoryApi.VictoryResult.customWin(CustomVictory.builder(
                            NoellesRoleRegistry.KILLER_QUEEN.identifier(), KillerQueenConstants.ROLE_COLOR)
                            .winners(winners).build());
                }
            }
            /* 未完成标记时绝不 KEEP_RUNNING，正常杀手/乘客结算可以继续。 */
            return VictoryApi.VictoryResult.pass();
        });
    }
}
