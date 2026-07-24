package org.agmas.noellesroles.roles.convener;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 召集者召集后的通讯限制判定。
 */
public final class ConvenerCommunicationHelper {
    private ConvenerCommunicationHelper() {
    }

    public static boolean isTemporarilySummonedLivingPlayer(PlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld.isRole(player, NoellesRoleRegistry.CONVENER)) {
            return false;
        }
        return ConvenerDisguiseComponent.KEY.get(player).getMorphTicks() > 0;
    }

    public static boolean shouldBlockVoiceBetween(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        return isTemporarilySummonedLivingPlayer(sender) && isTemporarilySummonedLivingPlayer(receiver);
    }

    public static boolean shouldRestrictChat(ServerPlayerEntity sender) {
        return isTemporarilySummonedLivingPlayer(sender);
    }

    public static boolean canReceiveRestrictedChat(ServerPlayerEntity recipient) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(recipient.getWorld());
        if (gameWorld.isRole(recipient, NoellesRoleRegistry.CONVENER)) {
            return true;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(recipient)) {
            return true;
        }
        return gameWorld.getRole(recipient) == null;
    }
}
