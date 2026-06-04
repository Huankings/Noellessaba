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
        handle(player, -1);
    }

    /**
     * 处理灵术师能力键。
     *
     * <p>附身和出窍共用同一枚 G 键。
     * 当客户端这一帧已经精准锁定到了玩家时，把目标实体 id 一并发给服务端，
     * 服务端只负责做目标合法性、距离和状态校验，
     * 这样就不会因为目标横向移动而把原本的“附身”误判成“出窍”。</p>
     *
     * @param clientTargetId 客户端当前锁定的玩家实体 id；没有则为 -1
     */
    public static void handle(ServerPlayerEntity player, int clientTargetId) {
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

        ServerPlayerEntity target = SpiritualistTargeting.getPossessionTarget(player, clientTargetId);

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
