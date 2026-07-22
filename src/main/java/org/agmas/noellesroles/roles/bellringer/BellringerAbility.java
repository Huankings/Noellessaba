package org.agmas.noellesroles.roles.bellringer;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;

/**
 * 敲钟人主动能力。
 */
public final class BellringerAbility {
    private BellringerAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(player, Noellesroles.BELLRINGER)
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            return;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.balance < BellringerConstants.ABILITY_PRICE) {
            return;
        }

        /*
         * 扣费、改时间和写冷却全部放在服务端最终校验之后。
         * 客户端 HUD 只负责提示，真正是否有钱、是否在对局中、是否仍是敲钟人，都以这里为准，
         * 避免客户端伪造能力包导致无成本改时间。
         */
        shop.balance -= BellringerConstants.ABILITY_PRICE;
        shop.sync();

        GameTimeComponent time = GameTimeComponent.KEY.get(player.getWorld());
        time.setTime(Math.max(0, time.getTime() - BellringerConstants.REDUCE_SECONDS * 20));
        player.playSoundToPlayer(SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        /*
         * 回放只保存稳定数值，不写死语言文本。
         * 最终展示文案由 NoellesRolesReplayFormatters 和 lang 文件决定，方便中英文切换。
         */
        NbtCompound extra = new NbtCompound();
        extra.putInt("seconds", BellringerConstants.REDUCE_SECONDS);
        extra.putInt("price", BellringerConstants.ABILITY_PRICE);
        GameRecordManager.recordSkillUse(player, Noellesroles.BELLRINGER_REDUCE_TIME_EVENT, null, extra);

        ability.setCooldown(BellringerConstants.ABILITY_COOLDOWN_TICKS);
    }
}
