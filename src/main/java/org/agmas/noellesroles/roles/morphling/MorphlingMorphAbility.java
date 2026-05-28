package org.agmas.noellesroles.roles.morphling;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;

public final class MorphlingMorphAbility {

    private MorphlingMorphAbility() {}

    /**
     * 处理变形怪的变形请求
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（变形怪）
     */
    public static void handle(MorphC2SPacket payload, ServerPlayerEntity player) {
        var world = player.getWorld();
        var gameWorld = GameWorldComponent.KEY.get(world);

        // 检查角色
        if (!gameWorld.isRole(player, Noellesroles.MORPHLING)) return;

        // 忽略卸除伪装请求（变形怪无此功能）
        if (payload.player().equals(player.getUuid())) return;

        var morphComp = MorphlingPlayerComponent.KEY.get(player);
        if (morphComp.getMorphTicks() != 0) return; // 变形中或冷却中

        morphComp.startMorph(payload.player());
    }
}
