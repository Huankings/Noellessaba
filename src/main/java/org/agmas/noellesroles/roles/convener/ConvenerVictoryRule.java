package org.agmas.noellesroles.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * 召集者活着时拖住普通杀手/乘客结算，并在成为唯一存活者时独立胜利。
 */
public final class ConvenerVictoryRule {
    private ConvenerVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(Identifier.of(NoellesRolesCore.MOD_ID, "victory/convener"), VictoryApi.DEFAULT_PRIORITY, context -> {
            ServerPlayerEntity livingConvener = ConvenerWinHelper.getLivingConvener(context.world(), context.gameWorld());
            if (livingConvener == null) {
                return VictoryApi.VictoryResult.pass();
            }

            if (context.alivePlayers().size() == 1) {
                return VictoryApi.VictoryResult.customWin(
                        CustomVictory.of(NoellesRoleRegistry.CONVENER.identifier(), NoellesRoleRegistry.CONVENER.color(), List.of((PlayerEntity) livingConvener))
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
