package org.agmas.noellesroles.roles.winder;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 风灵师主动能力：
 * 1. 冷却结束后按 G，开始漂浮；
 * 2. 漂浮期间再按 G，提前结束；
 * 3. 结束后再按实际持续时间换算冷却。
 */
public final class WinderAbility {

    private WinderAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.WINDER)
                || !gameWorld.isRunning()
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        WinderPlayerComponent winderComponent = WinderPlayerComponent.KEY.get(player);
        if (winderComponent.isFloatingActive()) {
            winderComponent.stopFloatingWithCooldown(true);
            return;
        }

        AbilityPlayerComponent abilityComponent = AbilityPlayerComponent.KEY.get(player);
        if (abilityComponent.cooldown > 0) {
            return;
        }

        winderComponent.startFloating();
    }
}
