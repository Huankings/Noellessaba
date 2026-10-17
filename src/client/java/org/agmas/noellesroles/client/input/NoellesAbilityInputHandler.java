package org.agmas.noellesroles.client.input;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.agmas.noellesroles.client.roles.angel.AngelAbilityInputHandler;
import org.agmas.noellesroles.client.roles.vulture.VultureAbilityInputHandler;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistAbilityInputHandler;
import org.agmas.noellesroles.client.roles.shadow_jester.ShadowJesterAbilityInputHandler;
import org.agmas.noellesroles.client.roles.vecna.VecnaAbilityInputHandler;
import org.agmas.noellesroles.client.roles.killer_queen.KillerQueenAbilityInputHandler;
import org.agmas.noellesroles.packet.host.AbilityC2SPacket;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/** 公共 G 能力键的按下分发器，具体职业输入仍由各职业 API 提供目标。 */
public final class NoellesAbilityInputHandler {
    private NoellesAbilityInputHandler() {
    }

    public static void tick(MinecraftClient client) {
        if (NoellesClientKeybindings.getAbilityBind() == null || !NoellesClientKeybindings.getAbilityBind().wasPressed()
                || client.player == null) {
            return;
        }
        if (VultureAbilityInputHandler.handleAbility(client)) {
            return;
        }
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (gameWorld.isRole(client.player, NoellesRoleRegistry.MYERS)) {
            return;
        }
        int targetId = -1;
        if (gameWorld.isRole(client.player, NoellesRoleRegistry.ANGEL)) {
            targetId = AngelAbilityInputHandler.resolveTargetId(client.player);
        } else if (gameWorld.isRole(client.player, NoellesRoleRegistry.SPIRITUALIST)) {
            targetId = SpiritualistAbilityInputHandler.resolveTargetId(client.player);
        } else if (gameWorld.isRole(client.player, NoellesRoleRegistry.SHADOW_JESTER)) {
            targetId = ShadowJesterAbilityInputHandler.resolveTargetId(client.player);
        } else if (gameWorld.isRole(client.player, NoellesRoleRegistry.VECNA)) {
            targetId = VecnaAbilityInputHandler.resolveTargetId(client.player);
        } else if (gameWorld.isRole(client.player, NoellesRoleRegistry.KILLER_QUEEN)) {
            targetId = KillerQueenAbilityInputHandler.resolveTargetId(client.player);
        }
        ClientPlayNetworking.send(new AbilityC2SPacket(targetId));
    }
}
