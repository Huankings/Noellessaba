package org.agmas.noellesroles.client.roles.perceiver;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.agmas.noellesroles.packet.role.perceiver.PerceiverAwarenessC2SPacket;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.perceiver.PerceiverConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 把只有本地客户端知道的本能输入/被动描边状态报告给服务端。
 *
 * <p>该类不判断最终凝视结果，也不授予任何能力；服务端仍会逐 tick 校验距离、视野角度、
 * 配置的连续凝视时间、普通肉眼可见、正常存活与停电状态。</p>
 */
public final class PerceiverClientPerceptionReporter {
    private static final Map<UUID, Boolean> LAST_REPORTED = new HashMap<>();
    private static int heartbeatTicks;

    private PerceiverClientPerceptionReporter() {
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
            LAST_REPORTED.clear();
            heartbeatTicks = 0;
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.world);
        if (!gameWorld.isRunning() || !GameFunctions.isPlayerAliveAndSurvival(client.player)) {
            LAST_REPORTED.clear();
            heartbeatTicks = 0;
            return;
        }

        heartbeatTicks++;
        boolean heartbeat = heartbeatTicks >= PerceiverConstants.AWARENESS_REPORT_INTERVAL_TICKS;
        if (heartbeat) {
            heartbeatTicks = 0;
        }

        Set<UUID> visiblePerceivers = new HashSet<>();
        for (AbstractClientPlayerEntity target : client.world.getPlayers()) {
            if (target == client.player
                    || !gameWorld.isRole(target, NoellesRoleRegistry.PERCEIVER)
                    || client.player.squaredDistanceTo(target) > PerceiverConstants.PERCEPTION_DISTANCE_SQUARED) {
                continue;
            }

            UUID perceiverUuid = target.getUuid();
            visiblePerceivers.add(perceiverUuid);
            boolean aware = WatheClient.isInstinctEnabled() || WatheClient.getInstinctHighlight(target) != -1;
            Boolean previous = LAST_REPORTED.put(perceiverUuid, aware);
            if (heartbeat || previous == null || previous != aware) {
                ClientPlayNetworking.send(new PerceiverAwarenessC2SPacket(perceiverUuid, aware));
            }
        }

        /*
         * 感知者离开客户端跟踪范围时主动发一次 false；即使包未送达，服务端 TTL 也会兜底过期。
         */
        for (UUID oldUuid : Set.copyOf(LAST_REPORTED.keySet())) {
            if (!visiblePerceivers.contains(oldUuid)) {
                ClientPlayNetworking.send(new PerceiverAwarenessC2SPacket(oldUuid, false));
                LAST_REPORTED.remove(oldUuid);
            }
        }
    }
}
