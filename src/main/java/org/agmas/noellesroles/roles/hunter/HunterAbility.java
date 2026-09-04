package org.agmas.noellesroles.roles.hunter;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;

/**
 * 追猎者主动能力：花费金币刷新匕首和猎刀冷却。
 */
public final class HunterAbility {
    private HunterAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(player, NoellesRoleRegistry.HUNTER)
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            return;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.balance < HunterConstants.ABILITY_PRICE) {
            return;
        }

        /*
         * 只在服务端最终确认“有钱、活着、确实是追猎者、能力没冷却”后扣费和刷新。
         * 客户端 HUD 只负责提示，不能决定是否真的能刷新武器冷却。
         */
        shop.balance -= HunterConstants.ABILITY_PRICE;
        shop.sync();
        player.getItemCooldownManager().set(WatheItems.KNIFE, 0);
        player.getItemCooldownManager().set(ModItems.HUNTING_KNIFE, 0);
        /*
         * 如果追猎者在开局 30 秒内花钱刷新猎刀冷却，服务端物品冷却已经被清零；
         * 刷新只需要清除真实物品冷却；tooltip 不再维护独立的开局来源标记。
         */
        player.playSoundToPlayer(SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

        NbtCompound extra = new NbtCompound();
        extra.putInt("price", HunterConstants.ABILITY_PRICE);
        GameRecordManager.recordSkillUse(player, NoellesEventIds.HUNTER_REFRESH_EVENT, null, extra);

        ability.setCooldown(HunterConstants.ABILITY_COOLDOWN_TICKS);
    }
}
