package org.agmas.noellesroles.roles.recaller;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

public final class RecallerAbility {

    private RecallerAbility() {}

    /**
     * 处理回溯者的技能使用
     * @param player 回溯者玩家
     */
    public static void handle(ServerPlayerEntity player) {
        var gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.RECALLER)) return;

        var ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) return;

        var recallerComp = RecallerPlayerComponent.KEY.get(player);
        var shop = PlayerShopComponent.KEY.get(player);

        if (!recallerComp.placed) {
            // 第一次使用：设置记录点
            ability.cooldown = RecallerConstants.SAVE_COOLDOWN_TICKS;
            recallerComp.setPosition();
            NbtCompound extra = new NbtCompound();
            extra.putInt("x", recallerComp.getSavedBlockX());
            extra.putInt("y", recallerComp.getSavedBlockY());
            extra.putInt("z", recallerComp.getSavedBlockZ());
            GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.RECALLER_POSITION_SAVED_EVENT, player, extra);
            ability.sync();
        } else {
            // 第二次使用：传送回记录点，需要花费固定金币。
            if (shop.balance >= RecallerConstants.TELEPORT_COST) {
                NbtCompound extra = new NbtCompound();
                extra.putInt("x", recallerComp.getSavedBlockX());
                extra.putInt("y", recallerComp.getSavedBlockY());
                extra.putInt("z", recallerComp.getSavedBlockZ());
                extra.putInt("cost", RecallerConstants.TELEPORT_COST);

                shop.balance -= RecallerConstants.TELEPORT_COST;
                shop.sync();
                ability.cooldown = RecallerConstants.TELEPORT_COOLDOWN_TICKS;
                recallerComp.teleport();
                GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.RECALLER_TELEPORTED_EVENT, player, extra);
                ability.sync();
            }
        }
    }
}
