package org.agmas.noellesroles.roles.operator;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.operator.OperatorC2SPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 接线员的主动能力服务端处理。
 *
 * <p>这里分为两条路径：</p>
 * <p>1. 选择两个不同玩家：接线</p>
 * <p>2. 两次都点同一玩家：广播</p>
 */
public final class OperatorAbility {

    private OperatorAbility() {
    }

    public static void handle(OperatorC2SPacket payload, ServerPlayerEntity operator) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(operator.getWorld());
        if (!gameWorld.isRole(operator, Noellesroles.OPERATOR)
                || !gameWorld.isRunning()
                || !GameFunctions.isPlayerAliveAndSurvival(operator)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(operator);
        if (ability.cooldown > 0) {
            return;
        }

        ServerPlayerEntity first = operator.getServer().getPlayerManager().getPlayer(payload.firstPlayer());
        ServerPlayerEntity second = operator.getServer().getPlayerManager().getPlayer(payload.secondPlayer());

        if (payload.firstPlayer().equals(payload.secondPlayer())) {
            handleBroadcast(operator, ability, payload.firstPlayer(), first);
        } else {
            handleConnection(operator, ability, payload.firstPlayer(), payload.secondPlayer(), first, second);
        }
    }

    private static void handleConnection(
            @NotNull ServerPlayerEntity operator,
            @NotNull AbilityPlayerComponent ability,
            @NotNull java.util.UUID firstUuid,
            @NotNull java.util.UUID secondUuid,
            @Nullable ServerPlayerEntity first,
            @Nullable ServerPlayerEntity second
    ) {
        boolean firstAlive = OperatorCommunicationManager.isLiveConnectionEndpoint(first);
        boolean secondAlive = OperatorCommunicationManager.isLiveConnectionEndpoint(second);

        if (firstAlive && secondAlive && first != null && second != null) {
            OperatorPlayerComponent.KEY.get(operator).startConnection(first, second);
            ability.setCooldown(OperatorConstants.CONNECTION_SUCCESS_COOLDOWN_TICKS);

            operator.sendMessage(
                    Text.translatable("message.noellesroles.operator.connection_selected", first.getDisplayName(), second.getDisplayName())
                            .withColor(Noellesroles.OPERATOR.color()),
                    true
            );
            operator.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);

            NbtCompound extra = new NbtCompound();
            extra.putUuid("player_one", first.getUuid());
            extra.putUuid("player_two", second.getUuid());
            extra.putString("player_one_name", first.getGameProfile().getName());
            extra.putString("player_two_name", second.getGameProfile().getName());
            GameRecordManager.recordGlobalEvent(operator.getServerWorld(), Noellesroles.OPERATOR_CONNECTION_STARTED_EVENT, operator, extra);
            return;
        }

        ability.setCooldown(OperatorConstants.CONNECTION_FAILURE_COOLDOWN_TICKS);
        operator.playSoundToPlayer(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        String firstName = resolveName(first, firstUuid);
        String secondName = resolveName(second, secondUuid);

        if (!firstAlive && !secondAlive) {
            operator.sendMessage(
                    Text.translatable("message.noellesroles.operator.connection_failed_both_dead")
                            .withColor(Noellesroles.OPERATOR.color()),
                    true
            );

            NbtCompound extra = new NbtCompound();
            extra.putUuid("player_one", firstUuid);
            extra.putUuid("player_two", secondUuid);
            extra.putString("player_one_name", firstName);
            extra.putString("player_two_name", secondName);
            GameRecordManager.recordGlobalEvent(operator.getServerWorld(), Noellesroles.OPERATOR_CONNECTION_FAILED_BOTH_DEAD_EVENT, operator, extra);
            return;
        }

        String deadName = !firstAlive ? firstName : secondName;
        operator.sendMessage(
                Text.translatable("message.noellesroles.operator.connection_failed_one_dead")
                        .withColor(Noellesroles.OPERATOR.color()),
                true
        );

        NbtCompound extra = new NbtCompound();
        extra.putUuid("player_one", firstUuid);
        extra.putUuid("player_two", secondUuid);
        extra.putString("player_one_name", firstName);
        extra.putString("player_two_name", secondName);
        extra.putString("dead_player_name", deadName);
        if (!firstAlive) {
            extra.putUuid("dead_player", firstUuid);
        } else {
            extra.putUuid("dead_player", secondUuid);
        }
        GameRecordManager.recordGlobalEvent(operator.getServerWorld(), Noellesroles.OPERATOR_CONNECTION_FAILED_ONE_DEAD_EVENT, operator, extra);
    }

    private static void handleBroadcast(
            @NotNull ServerPlayerEntity operator,
            @NotNull AbilityPlayerComponent ability,
            @NotNull java.util.UUID targetUuid,
            @Nullable ServerPlayerEntity target
    ) {
        if (OperatorCommunicationManager.isLiveConnectionEndpoint(target) && target != null) {
            OperatorPlayerComponent.KEY.get(operator).startBroadcast(target);
            ability.setCooldown(OperatorConstants.BROADCAST_SUCCESS_COOLDOWN_TICKS);

            operator.sendMessage(
                    Text.translatable("message.noellesroles.operator.broadcast_selected", target.getDisplayName())
                            .withColor(Noellesroles.OPERATOR.color()),
                    true
            );
            operator.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f);

            NbtCompound extra = new NbtCompound();
            extra.putUuid("target_player", target.getUuid());
            extra.putString("target_player_name", target.getGameProfile().getName());
            GameRecordManager.recordGlobalEvent(operator.getServerWorld(), Noellesroles.OPERATOR_BROADCAST_STARTED_EVENT, operator, extra);
            return;
        }

        ability.setCooldown(OperatorConstants.BROADCAST_FAILURE_COOLDOWN_TICKS);
        operator.sendMessage(
                Text.translatable("message.noellesroles.operator.broadcast_failed_dead")
                        .withColor(Noellesroles.OPERATOR.color()),
                true
        );
        operator.playSoundToPlayer(SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        NbtCompound extra = new NbtCompound();
        extra.putUuid("target_player", targetUuid);
        extra.putString("target_player_name", resolveName(target, targetUuid));
        if (target != null) {
            extra.putUuid("dead_player", target.getUuid());
            extra.putString("dead_player_name", target.getGameProfile().getName());
        } else {
            extra.putUuid("dead_player", targetUuid);
            extra.putString("dead_player_name", resolveName(null, targetUuid));
        }
        GameRecordManager.recordGlobalEvent(operator.getServerWorld(), Noellesroles.OPERATOR_BROADCAST_FAILED_EVENT, operator, extra);
    }

    private static @NotNull String resolveName(@Nullable ServerPlayerEntity player, @NotNull java.util.UUID uuid) {
        if (player != null) {
            return player.getGameProfile().getName();
        }
        return uuid.toString().substring(0, 8);
    }
}
