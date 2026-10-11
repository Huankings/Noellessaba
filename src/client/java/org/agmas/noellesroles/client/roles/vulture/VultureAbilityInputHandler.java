package org.agmas.noellesroles.client.roles.vulture;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.packet.role.vulture.VultureEatC2SPacket;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 秃鹫准心尸体缓存和能力键发送。 */
public final class VultureAbilityInputHandler {
    private static PlayerBodyEntity targetBody;

    private VultureAbilityInputHandler() {
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            targetBody = null;
            return;
        }
        targetBody = RoleNameHudApi.findLookedAtBody(client.player, RoleNameHudApi.defaultLookRange(client.player));
    }

    public static boolean handleAbility(MinecraftClient client) {
        if (client.player == null
                || !GameWorldComponent.KEY.get(client.player.getWorld()).isRole(client.player, NoellesRoleRegistry.VULTURE)
                || targetBody == null) {
            return false;
        }
        ClientPlayNetworking.send(new VultureEatC2SPacket(targetBody.getUuid()));
        return true;
    }

    public static void reset() {
        targetBody = null;
    }
}
