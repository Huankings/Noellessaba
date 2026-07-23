package org.agmas.noellesroles.roles.convener;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 召集者对尸体发动召集的服务端入口。
 */
public final class ConvenerSummonHandler {
    private static boolean initialized = false;

    private ConvenerSummonHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity convener) || !(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }
            if (!convener.interactionManager.getGameMode().isSurvivalLike()) {
                return ActionResult.PASS;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            if (!gameWorld.isRole(player, Noellesroles.CONVENER)) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof PlayerBodyEntity body)) {
                return ActionResult.PASS;
            }

            AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(convener);
            if (ability.cooldown > 0) {
                return ActionResult.PASS;
            }

            List<ServerPlayerEntity> alivePlayers = serverWorld.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
            if (alivePlayers.isEmpty()) {
                return ActionResult.PASS;
            }

            UUID disguiseTarget = body.getPlayerUuid();
            double targetX = body.getX();
            double targetY = body.getY();
            double targetZ = body.getZ();
            float targetYaw = body.getYaw();
            float targetPitch = body.getPitch();

            /*
             * 先移除尸体再结算，保证同一具尸体不会在高延迟或双击情况下被重复召集。
             */
            body.discard();
            ConvenerDisguiseComponent.KEY.get(convener).setPersistentDisguise(disguiseTarget);

            for (ServerPlayerEntity alivePlayer : alivePlayers) {
                alivePlayer.teleport(serverWorld, targetX, targetY, targetZ, Collections.emptySet(), targetYaw, targetPitch);
                if (alivePlayer == convener) {
                    continue;
                }

                PlayerPsychoComponent psycho = PlayerPsychoComponent.KEY.get(alivePlayer);
                if (psycho.getPsychoTicks() > 0) {
                    psycho.stopPsycho();
                    psycho.sync();
                }

                ConvenerSummonLockdownHelper.applySummonLockdown(alivePlayer);
                ConvenerDisguiseComponent.KEY.get(alivePlayer).setTimedDisguise(disguiseTarget, ConvenerConstants.SUMMON_MORPH_TICKS);
            }

            ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(convener);
            ConvenerMomentumComponent.KEY.get(convener).activate();
            convenerComponent.setRequiredSummons(ConvenerWinHelper.getRequiredSummons(serverWorld));
            convenerComponent.unlockDisguise(disguiseTarget);
            convenerComponent.incrementSummonCount();
            convenerComponent.sync();

            NbtCompound extra = new NbtCompound();
            extra.putUuid("corpse_owner", disguiseTarget);
            extra.putInt("summon_count", convenerComponent.getSummonCount());
            extra.putInt("required_summons", convenerComponent.getRequiredSummons());
            GameRecordManager.recordGlobalEvent(serverWorld, Noellesroles.CONVENER_SUMMON_EVENT, convener, extra);

            GameTimeComponent.KEY.get(serverWorld).addTime(ConvenerConstants.SUMMON_TIME_BONUS_TICKS);
            ability.setCooldown(ConvenerConstants.SUMMON_COOLDOWN_TICKS);

            if (convenerComponent.hasReachedSummonGoal()) {
                ConvenerWinHelper.declareConvenerWin(serverWorld, convener);
            }
            return ActionResult.CONSUME;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                if (gameWorld.isRole(player, Noellesroles.CONVENER)) {
                    ConvenerWinHelper.refreshRequiredSummons(player);
                }
            }
        });
    }
}
