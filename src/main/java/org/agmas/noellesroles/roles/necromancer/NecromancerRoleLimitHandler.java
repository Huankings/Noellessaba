package org.agmas.noellesroles.roles.necromancer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.Noellesroles;

/**
 * 死灵法师和扒手的动态生成上限。
 */
public final class NecromancerRoleLimitHandler {
    private static boolean initialized = false;

    private NecromancerRoleLimitHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var players = server.getPlayerManager().getPlayerList();
            if (players.isEmpty()) {
                return;
            }

            var world = players.getFirst().getWorld();
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            int killerRoleCount = (int) Math.floor((float) GameFunctions.getReadyPlayerCount(world) / (float) gameWorld.getKillerDividend());
            int maximum = killerRoleCount > 1 ? 1 : 0;

            /*
             * StupidExpress 原逻辑只有在杀手位多于 1 个时才允许刷出这两个特殊杀手。
             * 继续保留这个限制，避免小局里唯一杀手被替换成更偏辅助/经济的职业。
             */
            Harpymodloader.setRoleMaximum(Noellesroles.NECROMANCER, maximum);
            Harpymodloader.setRoleMaximum(Noellesroles.AVARICIOUS, maximum);
        });
    }
}
