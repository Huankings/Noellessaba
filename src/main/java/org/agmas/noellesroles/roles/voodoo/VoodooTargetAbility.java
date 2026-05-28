package org.agmas.noellesroles.roles.voodoo;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;

public final class VoodooTargetAbility {

    private VoodooTargetAbility() {}

    /**
     * 处理巫毒师的设置目标请求
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（巫毒师）
     */
    public static void handle(MorphC2SPacket payload, ServerPlayerEntity player) {
        var world = player.getWorld();
        var gameWorld = GameWorldComponent.KEY.get(world);

        // 检查角色
        if (!gameWorld.isRole(player, Noellesroles.VOODOO)) return;

        // 忽略卸除伪装请求（巫毒师无此功能）
        if (payload.player().equals(player.getUuid())) return;

        var ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) return;

        // 设置目标
        var voodooComp = VoodooPlayerComponent.KEY.get(player);
        voodooComp.setTarget(payload.player());

        /*
         * 绑定目标是巫毒师完整技能链的起点，
         * 因此在这里直接记录“把巫毒魔法绑给了谁”最准确。
         */
        NbtCompound extra = new NbtCompound();
        extra.putUuid("target_player", payload.player());
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.VOODOO_BOUND_EVENT, player, extra);

        // 设置冷却
        ability.cooldown = GameConstants.getInTicks(0, 30); // 30秒
        ability.sync();
    }
}
