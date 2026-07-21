package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;

/**
 * 平底锅蓄力时的挡枪逻辑。
 */
public final class CookDeathProtectionHandler {
    private CookDeathProtectionHandler() {
    }

    public static boolean allowDeath(PlayerEntity player, PlayerEntity killer, Identifier deathReason) {
        if (!GameConstants.DeathReasons.GUN.equals(deathReason)) {
            return true;
        }
        if (!player.getMainHandStack().isOf(ModItems.PAN)
                || !player.isUsingItem()
                || player.getActiveItem().getItem().getUseAction(player.getActiveItem()) != UseAction.SPEAR) {
            return true;
        }

        if (player instanceof ServerPlayerEntity victimPlayer) {
            NbtCompound blockedReplayData = GameFunctions.createBlockedDamageReplayData(killer, deathReason);
            GameRecordManager.recordShieldBlocked(
                    victimPlayer,
                    killer instanceof ServerPlayerEntity killerPlayer ? killerPlayer : null,
                    Noellesroles.PAN_SHIELD_SOURCE,
                    GameFunctions.getReplayItemId(blockedReplayData),
                    blockedReplayData
            );
        }

        consumePan(player);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        return false;
    }

    private static void consumePan(PlayerEntity player) {
        if (!player.isCreative()) {
            player.getMainHandStack().decrement(1);
        }
        /*
         * 创造/旁观玩家触发挡枪时多半是在调试死亡链，
         * 不写入冷却可以连续验证平底锅护盾表现。
         */
        if (!GameFunctions.isPlayerSpectatingOrCreative(player)) {
            player.getItemCooldownManager().set(ModItems.PAN, CookConstants.PAN_COOLDOWN_TICKS);
        }
        player.swingHand(Hand.MAIN_HAND, true);
    }
}
