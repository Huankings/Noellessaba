package org.agmas.noellesroles.client.bootstrap;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityClientSoundController;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.packet.role.jason.JasonAbilitySoundS2CPacket;
import org.agmas.noellesroles.packet.role.spiritualist.SpiritualistPossessionViewS2CPacket;

/** 客户端服务端包接收注册。具体状态处理仍留在对应职业包中。 */
public final class NoellesClientNetworkingBootstrap {
    private NoellesClientNetworkingBootstrap() {
    }

    public static void register() {
        SpiritualistClientController.init();
        ClientPlayNetworking.registerGlobalReceiver(SpiritualistPossessionViewS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> SpiritualistClientController.handlePossessionViewPacket(payload)));
        ClientPlayNetworking.registerGlobalReceiver(JasonAbilitySoundS2CPacket.ID, (payload, context) ->
                context.client().execute(() -> JasonAbilityClientSoundController.handle(context.client(), payload.action())));
    }
}
