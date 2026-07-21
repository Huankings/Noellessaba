package org.agmas.noellesroles.mixin.roles.avaricious;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.avaricious.AvariciousConstants;
import org.agmas.noellesroles.roles.avaricious.AvariciousPayoutComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MurderGameMode.class)
public class AvariciousGoldPayoutMixin {
    @Inject(method = "tickServerGameLoop", at = @At("TAIL"))
    private void noellesroles$payoutAvariciousGold(
            ServerWorld serverWorld,
            GameWorldComponent gameWorldComponent,
            CallbackInfo ci
    ) {
        GameTimeComponent timeComponent = GameTimeComponent.KEY.get(serverWorld);
        int time = timeComponent.time;

        AvariciousPayoutComponent payoutComponent = AvariciousPayoutComponent.KEY.get(serverWorld);
        if (!payoutComponent.hasTimerStartTime()) {
            /*
             * Wathe 的时间是剩余时间，所以先记录第一次进入结算逻辑时的剩余时间。
             * 服务端发钱和客户端 HUD 都按这个起点算 elapsed，避免玩家看到的倒计时和真实发钱 tick 错开。
             */
            payoutComponent.setTimerStartTime(time);
            return;
        }

        int elapsed = payoutComponent.getTimerStartTime() - time;
        if (elapsed % AvariciousConstants.TIMER_TICKS != 0) {
            return;
        }

        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (!gameWorldComponent.isRole(player, Noellesroles.AVARICIOUS)) {
                continue;
            }

            int nearbyPlayers = 0;
            for (ServerPlayerEntity other : serverWorld.getPlayers()) {
                if (other == player || GameFunctions.isPlayerEliminated(other)) {
                    continue;
                }
                if (other.distanceTo(player) <= AvariciousConstants.MAX_DISTANCE) {
                    nearbyPlayers++;
                }
            }

            if (nearbyPlayers <= 0) {
                continue;
            }

            /*
             * 回放只记录本次结算总额，不记录每个受害者。
             * 这样和 StupidExpress 当前实现保持一致，也能避免玩家掉线后名单格式化复杂化。
             */
            int stolenAmount = nearbyPlayers * AvariciousConstants.PAYOUT_PER_PLAYER;
            PlayerShopComponent.KEY.get(player).addToBalance(stolenAmount);
            player.playSoundToPlayer(WatheSounds.UI_SHOP_BUY, SoundCategory.PLAYERS, 10.0F, 0.5F);

            NbtCompound extra = new NbtCompound();
            extra.putInt("amount", stolenAmount);
            GameRecordManager.recordGlobalEvent(serverWorld, Noellesroles.AVARICIOUS_STOLE_COINS_EVENT, player, extra);
        }
    }
}
