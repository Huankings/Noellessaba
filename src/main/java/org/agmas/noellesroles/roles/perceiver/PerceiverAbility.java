package org.agmas.noellesroles.roles.perceiver;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.packet.role.perceiver.PerceiverAwarenessC2SPacket;
import org.agmas.noellesroles.packet.role.perceiver.PerceiverSelectC2SPacket;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 感知者背包选人能力与客户端感知资格报告的服务端入口。 */
public final class PerceiverAbility {
    private PerceiverAbility() {
    }

    public static void handleSelection(@NotNull PerceiverSelectC2SPacket payload, @NotNull ServerPlayerEntity perceiver) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(perceiver.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(perceiver, NoellesRoleRegistry.PERCEIVER)
                || !GameFunctions.isPlayerAliveAndSurvival(perceiver)
                || TimekeeperPlayerComponent.KEY.get(perceiver).isInTimeRift()
                || perceiver.getUuid().equals(payload.targetPlayer())) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(perceiver);
        if (ability.cooldown > 0) {
            return;
        }

        ServerPlayerEntity target = perceiver.getServer().getPlayerManager().getPlayer(payload.targetPlayer());
        if (target != null
                && GameFunctions.isPlayerAliveAndSurvival(target)
                && !TimekeeperPlayerComponent.KEY.get(target).isInTimeRift()) {
            PerceiverPlayerComponent.KEY.get(perceiver).startSelectedEffect(target);
            ability.setCooldown(PerceiverConstants.SUCCESS_COOLDOWN_TICKS);
            perceiver.sendMessage(
                    Text.translatable("message.noellesroles.perceiver.selection_succeeded", target.getDisplayName())
                            .withColor(PerceiverConstants.ROLE_COLOR),
                    true
            );
            perceiver.playSoundToPlayer(
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS,
                    PerceiverConstants.SUCCESS_SOUND_VOLUME,
                    PerceiverConstants.SUCCESS_SOUND_PITCH
            );

            NbtCompound extra = targetData(target.getUuid(), target.getGameProfile().getName());
            GameRecordManager.recordGlobalEvent(
                    perceiver.getServerWorld(),
                    NoellesEventIds.PERCEIVER_SELECTED_EFFECT_STARTED_EVENT,
                    perceiver,
                    extra
            );
            return;
        }

        ability.setCooldown(PerceiverConstants.FAILURE_COOLDOWN_TICKS);
        perceiver.sendMessage(
                Text.translatable("message.noellesroles.perceiver.selection_failed_dead")
                        .withColor(PerceiverConstants.ROLE_COLOR),
                true
        );
        perceiver.playSoundToPlayer(
                SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.PLAYERS,
                PerceiverConstants.FAILURE_SOUND_VOLUME,
                PerceiverConstants.FAILURE_SOUND_PITCH
        );

        String targetName = resolveName(target, payload.targetPlayer());
        NbtCompound extra = targetData(payload.targetPlayer(), targetName);
        extra.putUuid("dead_player", payload.targetPlayer());
        extra.putString("dead_player_name", targetName);
        GameRecordManager.recordGlobalEvent(
                perceiver.getServerWorld(),
                NoellesEventIds.PERCEIVER_SELECTION_FAILED_EVENT,
                perceiver,
                extra
        );
    }

    public static void handleAwarenessReport(
            @NotNull PerceiverAwarenessC2SPacket payload,
            @NotNull ServerPlayerEntity viewer
    ) {
        ServerPlayerEntity perceiver = viewer.getServer().getPlayerManager().getPlayer(payload.perceiverPlayer());
        if (perceiver == null || perceiver.getWorld() != viewer.getWorld()) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(perceiver.getWorld());
        if (!gameWorld.isRunning() || !gameWorld.isRole(perceiver, NoellesRoleRegistry.PERCEIVER)) {
            return;
        }

        boolean validViewer = GameFunctions.isPlayerAliveAndSurvival(viewer)
                && !TimekeeperPlayerComponent.KEY.get(viewer).isInTimeRift();
        PerceiverPlayerComponent.KEY.get(perceiver)
                .updateAwarenessReport(viewer.getUuid(), payload.aware() && validViewer);
    }

    private static @NotNull NbtCompound targetData(@NotNull UUID targetUuid, @NotNull String targetName) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("target_player", targetUuid);
        extra.putString("target_player_name", targetName);
        return extra;
    }

    private static @NotNull String resolveName(@Nullable ServerPlayerEntity target, @NotNull UUID targetUuid) {
        return target == null ? targetUuid.toString().substring(0, 8) : target.getGameProfile().getName();
    }
}
