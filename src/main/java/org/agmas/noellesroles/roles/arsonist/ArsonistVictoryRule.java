package org.agmas.noellesroles.roles.arsonist;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * 纵火犯可选的独立保活胜利规则。
 */
public final class ArsonistVictoryRule {
    private ArsonistVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(IdentifierHolder.ID, VictoryApi.DEFAULT_PRIORITY, context -> {
            if (!ArsonistConstants.KEEPS_GAME_GOING) {
                return VictoryApi.VictoryResult.pass();
            }

            List<ServerPlayerEntity> arsonists = context.alivePlayers().stream()
                    .filter(player -> context.gameWorld().isRole(player, NoellesRoleRegistry.ARSONIST))
                    .toList();
            if (arsonists.isEmpty()) {
                return VictoryApi.VictoryResult.pass();
            }

            if (context.alivePlayers().size() == 1) {
                return VictoryApi.VictoryResult.customWin(
                        CustomVictory.of(NoellesRoleRegistry.ARSONIST.identifier(), NoellesRoleRegistry.ARSONIST.color(), List.copyOf(arsonists))
                );
            }

            if (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS
                    || context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS) {
                return VictoryApi.VictoryResult.keepRunning();
            }
            return VictoryApi.VictoryResult.pass();
        });
    }

    private static final class IdentifierHolder {
        private static final net.minecraft.util.Identifier ID = net.minecraft.util.Identifier.of(NoellesRolesCore.MOD_ID, "victory/arsonist");
    }
}
