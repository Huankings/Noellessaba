package org.agmas.noellesroles.roles.insane_damned_paranoid_killer;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 亡语杀手能力键逻辑。
 */
public final class InsaneDamnedKillerAbility {
    private InsaneDamnedKillerAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, NoellesRoleRegistry.THE_INSANE_DAMNED_PARANOID_KILLER_OF_DOOM_DEATH_DESTRUCTION_AND_WAFFLES)
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        /*
         * spark 版尸体伪装是“按一次开启，再按一次关闭”的纯开关。
         * 本需求只替换减速实现，不增加持续时间、冷却或回放记录。
         */
        InsaneDamnedKillerPlayerComponent.KEY.get(player).toggleCorpseMode();
    }
}
