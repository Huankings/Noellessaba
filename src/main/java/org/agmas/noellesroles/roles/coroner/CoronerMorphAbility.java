package org.agmas.noellesroles.roles.coroner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;

public final class CoronerMorphAbility {

    private CoronerMorphAbility() {}

    /**
     * 处理验尸官的变形/卸除伪装请求
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（验尸官）
     */
    public static void handle(MorphC2SPacket payload, ServerPlayerEntity player) {
        var world = player.getWorld();
        var gameWorld = GameWorldComponent.KEY.get(world);

        // 检查角色
        if (!gameWorld.isRole(player, Noellesroles.CORONER)) return;

        var coronerComp = CoronerPlayerComponent.KEY.get(player);

        // 判断是否为卸除伪装请求（发送自己的UUID）
        boolean isRemoveRequest = payload.player().equals(player.getUuid());

        if (isRemoveRequest) {
            // 卸除伪装
            if (coronerComp.disguise != null && coronerComp.getMorphTicks() > 0) {
                coronerComp.removeDisguise(); // 使用组件中的专用方法
            }
            return;
        }

        // 正常的变形请求：目标玩家必须存在且为旁观者
        PlayerEntity targetPlayer = world.getPlayerByUuid(payload.player());
        if (targetPlayer == null) return;

      //  if (!targetPlayer.isSpectator()) {
      //      player.sendMessage(Text.literal("§c只能变形死亡玩家").formatted(Formatting.RED), true);
      //      return;
      //  }

        if (coronerComp.getMorphTicks() < 0) return; // 冷却中

        coronerComp.startMorph(payload.player());
    }
}