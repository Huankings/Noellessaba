package org.agmas.noellesroles.roles.muzzler;

import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;

/**
 * 静语者“撕下胶带”交互。
 */
public final class MuzzlerInteractionHandler {
    private static final SoundEvent TAPE_APPLY_SOUND =
            SoundEvent.of(Identifier.of(NoellesRolesCore.MOD_ID, "item.tape.apply"));
    private static boolean initialized = false;

    private MuzzlerInteractionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof PlayerEntity victim)) {
                return ActionResult.PASS;
            }
            if (MuzzlerConstants.TAPE_TEAR_CHECK_COUNT == 0) {
                return ActionResult.PASS;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            if (!gameWorld.isRunning()
                    || !GameFunctions.isPlayerAliveAndSurvival(player)
                    || !GameFunctions.isPlayerAliveAndSurvival(victim)) {
                return ActionResult.PASS;
            }

            /*
             * 手持胶带对玩家右键属于“贴胶带”，不能同时被这里当成“帮别人撕胶带”。
             * 该判断沿用 StarryExpress 原逻辑，统一只看主手，避免副手触发造成双语义。
             */
            if (player.getMainHandStack().isOf(ModItems.TAPE)) {
                return ActionResult.PASS;
            }

            SilencePlayerComponent victimSilence = SilencePlayerComponent.KEY.get(victim);
            if (!victimSilence.isSilenced() || SilencePlayerComponent.KEY.get(player).isSilenced()) {
                return ActionResult.PASS;
            }

            if (world.isClient) {
                return ActionResult.SUCCESS;
            }

            victimSilence.setTearChecks(victimSilence.getTearChecks() + 1);
            victim.getWorld().playSound(
                    null,
                    victim.getX(),
                    victim.getY(),
                    victim.getZ(),
                    TAPE_APPLY_SOUND,
                    SoundCategory.PLAYERS,
                    1.0F,
                    2.0F
            );
            recordTapeRemoved(player, victim, victimSilence);

            if (victimSilence.getTearChecks() >= MuzzlerConstants.TAPE_TEAR_CHECK_COUNT) {
                victimSilence.setSilenced(false);
            }
            victimSilence.sync();

            PlayerMoodComponent victimMood = PlayerMoodComponent.KEY.get(victim);
            victimMood.setMood(victimMood.getMood() - MuzzlerConstants.TAPE_TEAR_MOOD_CHANGE);
            victimMood.sync();

            if (victimMood.getMood() <= 0.0F && MuzzlerConstants.KILL_IF_CHECKED_AT_ZERO) {
                killLowMoodVictim(player, victim, victimSilence);
            }

            return ActionResult.SUCCESS;
        });
    }

    private static void recordTapeRemoved(PlayerEntity remover, PlayerEntity victim, SilencePlayerComponent victimSilence) {
        if (!(remover instanceof ServerPlayerEntity serverRemover) || !(victim instanceof ServerPlayerEntity serverVictim)) {
            return;
        }

        GameRecordManager.EventBuilder event = GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                .world(serverRemover.getServerWorld())
                .actor(serverRemover)
                .target(serverVictim)
                .put("event", NoellesEventIds.TAPE_REMOVED_EVENT.toString());
        if (victimSilence.getSilencer() != null) {
            event.putUuid("silencer", victimSilence.getSilencer());
        }
        event.record();
    }

    private static void killLowMoodVictim(PlayerEntity remover, PlayerEntity victim, SilencePlayerComponent victimSilence) {
        NbtCompound extraDeathData = new NbtCompound();
        PlayerEntity killer = null;
        if (victimSilence.getSilencer() != null) {
            extraDeathData.putUuid("silencer", victimSilence.getSilencer());
            extraDeathData.putUuid("replay_actor", victimSilence.getSilencer());
            killer = victim.getWorld().getPlayerByUuid(victimSilence.getSilencer());
        }
        extraDeathData.putUuid("remover", remover.getUuid());
        GameFunctions.killPlayer(
                victim,
                true,
                killer,
                NoellesDeathReasons.SILENCED_TAPE_REMOVED_DEATH_REASON,
                extraDeathData
        );
    }
}
