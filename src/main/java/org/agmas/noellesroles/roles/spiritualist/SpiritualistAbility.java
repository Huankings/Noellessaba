package org.agmas.noellesroles.roles.spiritualist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 灵术师能力键入口。
 *
 * <p>技能键只有一枚，但会根据当前状态和准心目标分流成 4 种行为：
 * 1. 正常状态 + 未对准玩家：开始灵魂出窍；
 * 2. 正常状态 + 对准玩家：开始附身；
 * 3. 出窍中：结束出窍；
 * 4. 附身中：结束附身。</p>
 */
public final class SpiritualistAbility {

    private SpiritualistAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.SPIRITUALIST)
                || !gameWorld.isRunning()
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        SpiritualistPlayerComponent spiritualist = SpiritualistPlayerComponent.KEY.get(player);
        if (spiritualist.isProjecting()) {
            SpiritualistManager.endProjection(player, true);
            return;
        }

        if (spiritualist.isPossessing()) {
            SpiritualistManager.endPossession(player, true, true, true);
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            return;
        }

        ServerPlayerEntity target = SpiritualistTargeting.getPossessionTarget(player) instanceof ServerPlayerEntity serverTarget
                ? serverTarget
                : null;

        if (target == null) {
            SpiritualistManager.startProjection(player);
            return;
        }

        if (SpiritualistManager.isPsychoTarget(target)) {
            SpiritualistManager.sendRoleActionbar(player, "message.noellesroles.spiritualist.cannot_possess_psycho");
            return;
        }

        SpiritualistHostComponent hostComponent = SpiritualistHostComponent.KEY.get(target);
        if (hostComponent.possessed) {
            return;
        }

        SpiritualistManager.startPossession(player, target);
    }
}
