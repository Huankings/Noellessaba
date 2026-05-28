package org.agmas.noellesroles.roles.controller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.controller.ControllerReleaseC2SPacket;

public final class ControllerReleaseAbility {

    private ControllerReleaseAbility() {
        // 工具类，禁止实例化
    }

    /**
     * 处理附体师的解除附体请求
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（附体师）
     */
    public static void handle(ControllerReleaseC2SPacket payload, ServerPlayerEntity player) {
        var world = player.getWorld();
        var gameWorld = GameWorldComponent.KEY.get(world);

        // 检查角色
        if (!gameWorld.isRole(player, Noellesroles.CONTROLLER)) return;

        ControllerPlayerComponent controllerComp = ControllerPlayerComponent.KEY.get(player);
        if (controllerComp.controlledTarget != null && controllerComp.possessTicks > 0) {
            // 这里是玩家主动点击自己头像提前解除附体的唯一路径，
            // 因此显式标记 manual=true，避免和自然结束混淆。
            controllerComp.releasePossession(false, true);
        }
    }
}
