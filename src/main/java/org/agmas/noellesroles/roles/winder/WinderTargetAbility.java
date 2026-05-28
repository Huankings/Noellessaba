package org.agmas.noellesroles.roles.winder;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;

/**
 * 风灵师头像选人处理。
 * 这里不限制目标是否已死亡，也不设置选择冷却。
 */
public final class WinderTargetAbility {

    private WinderTargetAbility() {
    }

    public static void handle(MorphC2SPacket payload, ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.WINDER)) {
            return;
        }

        WinderPlayerComponent.KEY.get(player).setSelectedTarget(payload.player());
    }
}
