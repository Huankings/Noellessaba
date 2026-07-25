package org.agmas.noellesroles.roles.initiate;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 初学者随机生成上限。
 *
 * <p>Harpy 只随机分配一个初学者名额，第二个初学者由配对 mixin 从另一名中立玩家补齐。
 * 这样可以保持原 StupidExpress 的“只有杀手位足够多时才出现一对初学者”的节奏。</p>
 */
public final class InitiateRoleLimitHandler {
    private static boolean initialized = false;

    private InitiateRoleLimitHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var world = server.getOverworld();
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            int killerSlots = (int) Math.floor((float) GameFunctions.getReadyPlayerCount(world) / (float) gameWorld.getKillerDividend());
            Harpymodloader.setRoleMaximum(
                    NoellesRoleRegistry.INITIATE,
                    killerSlots >= InitiateConstants.MIN_KILLER_SLOTS_FOR_PAIR ? 1 : 0
            );
        });
    }
}
