package org.agmas.noellesroles.roles.arsonist;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import org.agmas.noellesroles.ModItems;

import java.util.List;

/**
 * 纵火犯用汽油桶给玩家浇油。
 */
public final class OilDousingHandler {
    private static boolean initialized = false;

    private OilDousingHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity arsonist)) {
                return ActionResult.PASS;
            }
            if (!arsonist.interactionManager.getGameMode().isSurvivalLike()) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            if (!gameWorld.isRole(player, NoellesRoleRegistry.ARSONIST)) {
                return ActionResult.PASS;
            }
            if (!player.getStackInHand(hand).isOf(ModItems.JERRY_CAN)) {
                return ActionResult.PASS;
            }

            boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
            if (!ignoresCooldown && arsonist.getItemCooldownManager().isCoolingDown(ModItems.JERRY_CAN)) {
                return ActionResult.PASS;
            }

            if (!ignoresCooldown && world instanceof ServerWorld serverWorld) {
                List<ServerPlayerEntity> alivePlayers = serverWorld.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
                int cooldownTicks = ArsonistConstants.getDouseCooldownTicks(alivePlayers.size());
                long dousedCount = alivePlayers.stream().filter(p -> DousedPlayerComponent.KEY.get(p).isDoused()).count();

                arsonist.getItemCooldownManager().set(ModItems.JERRY_CAN, cooldownTicks);
                if (dousedCount >= ArsonistConstants.getRequiredDousedCount(alivePlayers.size())) {
                    boolean lighterWasCoolingDown = arsonist.getItemCooldownManager().isCoolingDown(ModItems.LIGHTER);
                    arsonist.getItemCooldownManager().set(ModItems.LIGHTER, cooldownTicks);
                    if (!lighterWasCoolingDown) {
                        GameRecordManager.recordGlobalEvent(arsonist.getServerWorld(), NoellesEventIds.ARSONIST_LIGHTER_COOLDOWN_STARTED_EVENT, arsonist, null);
                        ArsonistReplayTracker.trackLighterCooldown(arsonist);
                    }
                }
            }

            DousedPlayerComponent doused = DousedPlayerComponent.KEY.get(target);
            doused.setDoused(true);
            doused.sync();

            NbtCompound extra = new NbtCompound();
            extra.putUuid("target_player", target.getUuid());
            GameRecordManager.recordGlobalEvent(arsonist.getServerWorld(), NoellesEventIds.ARSONIST_DOUSED_EVENT, arsonist, extra);

            arsonist.playSoundToPlayer(SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.PLAYERS, 1.0F, 1.0F);
            return ActionResult.CONSUME;
        });
    }
}
