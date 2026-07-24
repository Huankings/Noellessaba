package org.agmas.noellesroles.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 召集者胜利相关的统一出口。
 */
public final class ConvenerWinHelper {
    private ConvenerWinHelper() {
    }

    public static int getRequiredSummons(@NotNull World world) {
        return Math.max(1, (getRoundPlayerCount(world) / 3) + 1);
    }

    /**
     * 按三种来源取最大值，避免游戏开始后 ready 区人数归零导致目标次数错误变成 1。
     */
    public static int getRoundPlayerCount(@NotNull World world) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        int assignedRoleCount = gameWorld.getRoles().size();
        int alivePlayerCount = (int) world.getPlayers().stream().filter(GameFunctions::isPlayerAliveAndSurvival).count();
        int readyPlayerCount = GameFunctions.getReadyPlayerCount(world);
        return Math.max(1, Math.max(assignedRoleCount, Math.max(alivePlayerCount, readyPlayerCount)));
    }

    public static void refreshRequiredSummons(@NotNull ServerPlayerEntity convener) {
        ConvenerPlayerComponent component = ConvenerPlayerComponent.KEY.get(convener);
        int requiredSummons = getRequiredSummons(convener.getWorld());
        if (component.getRequiredSummons() == requiredSummons) {
            return;
        }
        component.setRequiredSummons(requiredSummons);
        component.sync();
    }

    public static @Nullable ServerPlayerEntity getLivingConvener(@NotNull ServerWorld world, @NotNull GameWorldComponent gameWorld) {
        for (ServerPlayerEntity player : world.getPlayers(GameFunctions::isPlayerAliveAndSurvival)) {
            if (gameWorld.isRole(player, NoellesRoleRegistry.CONVENER)) {
                return player;
            }
        }
        return null;
    }

    public static void declareConvenerWin(@NotNull ServerWorld world, @NotNull ServerPlayerEntity winner) {
        VictoryApi.endGameWithCustomVictory(
                world,
                CustomVictory.of(NoellesRoleRegistry.CONVENER.identifier(), NoellesRoleRegistry.CONVENER.color(), List.of((PlayerEntity) winner))
        );
    }
}
