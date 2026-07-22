package org.agmas.noellesroles.roles.cleaner;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 清道夫主动能力：花费金币清除场上的掉落物。
 */
public final class CleanerAbility {
    private CleanerAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(player, Noellesroles.CLEANER)
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            return;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.balance < CleanerConstants.ABILITY_PRICE) {
            return;
        }

        /*
         * kinssaba 原版通过命令 kill @e[type=item] 清掉落物。
         * 迁入 NoellesRoles 后直接遍历服务端世界里的 ItemEntity，可以得到同等效果，
         * 同时避免命令上下文、权限和跨维度选择器行为受服务器配置影响。
         */
        int clearedItems = 0;
        for (ServerWorld world : player.getServer().getWorlds()) {
            for (var itemEntity : world.getEntitiesByType(EntityType.ITEM, ignored -> true)) {
                itemEntity.discard();
                clearedItems++;
            }
        }

        shop.balance -= CleanerConstants.ABILITY_PRICE;
        shop.sync();
        player.playSoundToPlayer(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.0F, 1.0F);

        /*
         * 回放只保存稳定数值，不保存中英文文本。
         * cleared_items 目前只是调试/扩展字段，正式显示仍沿用 kinssaba 的“花费金币清场”文案。
         */
        NbtCompound extra = new NbtCompound();
        extra.putInt("price", CleanerConstants.ABILITY_PRICE);
        extra.putInt("cleared_items", clearedItems);
        GameRecordManager.recordSkillUse(player, Noellesroles.CLEANER_CLEAR_ITEMS_EVENT, null, extra);

        ability.setCooldown(CleanerConstants.ABILITY_COOLDOWN_TICKS);
    }
}
