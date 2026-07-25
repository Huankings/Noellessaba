package org.agmas.noellesroles.roles.licensed_villain;

import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.List;

/**
 * 执照恶棍的独立胜利和保活规则。
 *
 * <p>kinssaba 旧版曾经通过 mixin 卡 MurderGameMode 的胜利局部变量；
 * 当前 Wathe 已提供 VictoryApi，所以这里直接用公开接口表达规则：
 * 执照恶棍活着时阻止普通杀手/乘客提前结算，只剩自己时触发独立胜利。</p>
 */
public final class LicensedVillainVictoryRule {
    private LicensedVillainVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(NoellesRolesCore.id("victory/licensed_villain"), VictoryApi.DEFAULT_PRIORITY, context -> {
            List<ServerPlayerEntity> licensedVillains = context.alivePlayers().stream()
                    .filter(player -> context.gameWorld().isRole(player, NoellesRoleRegistry.LICENSED_VILLAIN))
                    .toList();
            if (licensedVillains.isEmpty()) {
                return VictoryApi.VictoryResult.pass();
            }

            if (context.alivePlayers().size() == 1) {
                return VictoryApi.VictoryResult.customWin(CustomVictory.of(
                        NoellesRoleRegistry.LICENSED_VILLAIN.identifier(),
                        NoellesRoleRegistry.LICENSED_VILLAIN.color(),
                        licensedVillains
                ));
            }

            if (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS
                    || context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS) {
                return VictoryApi.VictoryResult.keepRunning();
            }
            return VictoryApi.VictoryResult.pass();
        });
    }
}
