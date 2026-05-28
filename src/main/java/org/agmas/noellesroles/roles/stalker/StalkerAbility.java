package org.agmas.noellesroles.roles.stalker;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.stalker.StalkerDashC2SPacket;
import org.agmas.noellesroles.packet.role.stalker.StalkerGazeC2SPacket;

public final class StalkerAbility {

    public static void handleGaze(StalkerGazeC2SPacket payload, ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.STALKER)) return;
        StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(player);
        if (payload.gazing()) {
            comp.startGazing();
        } else {
            comp.stopGazing();
        }
    }

    public static void handleDash(StalkerDashC2SPacket payload, ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.STALKER)) return;
        StalkerPlayerComponent comp = StalkerPlayerComponent.KEY.get(player);
        if (payload.charging()) {
            comp.startCharging();
        } else {
            comp.releaseCharge();
        }
    }
}