package org.agmas.noellesroles.roles.arsonist;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.ModItems;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * 纵火犯打火机冷却结束回放追踪。
 */
public final class ArsonistReplayTracker {
    private static final Set<UUID> TRACKED_LIGHTER_COOLDOWNS = new HashSet<>();
    private static boolean initialized = false;

    private ArsonistReplayTracker() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (TRACKED_LIGHTER_COOLDOWNS.isEmpty()) {
                return;
            }

            Iterator<UUID> iterator = TRACKED_LIGHTER_COOLDOWNS.iterator();
            while (iterator.hasNext()) {
                UUID playerUuid = iterator.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                if (player == null || GameFunctions.isPlayerSpectatingOrCreative(player)) {
                    iterator.remove();
                    continue;
                }

                GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
                if (!gameWorld.isRunning()
                        || !gameWorld.isRole(player, NoellesRoleRegistry.ARSONIST)
                        || !GameFunctions.isPlayerAliveAndSurvival(player)) {
                    iterator.remove();
                    continue;
                }

                if (player.getItemCooldownManager().isCoolingDown(ModItems.LIGHTER)) {
                    continue;
                }

                GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.ARSONIST_LIGHTER_COOLDOWN_FINISHED_EVENT, player, null);
                iterator.remove();
            }
        });
    }

    public static void trackLighterCooldown(ServerPlayerEntity player) {
        TRACKED_LIGHTER_COOLDOWNS.add(player.getUuid());
    }
}
