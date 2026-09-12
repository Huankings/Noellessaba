package org.agmas.noellesroles.roles.outlaw;

import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.Comparator;
import java.util.List;

/** 亡命徒的独立胜利和“活着时阻止其它结算”规则。 */
public final class OutlawVictoryRule {
    private static boolean initialized;

    private OutlawVictoryRule() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        VictoryApi.registerRule(NoellesRolesCore.id("victory/outlaw"), 300, context -> {
            List<ServerPlayerEntity> outlaws = context.alivePlayers().stream()
                    .filter(player -> context.gameWorld().isRole(player, NoellesRoleRegistry.OUTLAW)
                            && OutlawPlayerComponent.KEY.get(player).isOutlawActive())
                    .sorted(Comparator.comparing(player -> player.getUuid().toString()))
                    .toList();
            if (outlaws.isEmpty()) {
                return VictoryApi.VictoryResult.pass();
            }

            // 测试导致多个亡命徒同时存在时，只取 UUID 最小者作为唯一独立赢家。
            if (outlaws.size() == context.alivePlayers().size()) {
                return VictoryApi.VictoryResult.customWin(CustomVictory.of(
                        NoellesRoleRegistry.OUTLAW.identifier(),
                        NoellesRoleRegistry.OUTLAW.color(),
                        List.of(outlaws.getFirst())
                ));
            }

            if (context.vanillaWinStatus() != GameFunctions.WinStatus.NONE) {
                return VictoryApi.VictoryResult.keepRunning();
            }
            return VictoryApi.VictoryResult.pass();
        });
    }
}
