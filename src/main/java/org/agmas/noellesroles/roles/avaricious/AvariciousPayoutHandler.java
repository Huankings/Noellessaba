package org.agmas.noellesroles.roles.avaricious;

import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

/**
 * 扒手的周期金币结算。
 *
 * <p>旧实现注入在 MurderGameMode#tickServerGameLoop 的 TAIL。
 * 胜利 API 的提前 return 会跳过这个 TAIL，导致计时和发钱完全停止；
 * 这里改用独立服务端 tick，保证特殊胜利规则不会影响经济机制。</p>
 */
public final class AvariciousPayoutHandler {
    private static boolean initialized;

    private AvariciousPayoutHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(AvariciousPayoutHandler::tickServer);
        GameEvents.ON_FINISH_INITIALIZE.register((world, gameWorld) -> {
            if (world instanceof ServerWorld serverWorld) {
                AvariciousPayoutComponent.KEY.get(serverWorld).reset();
            }
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, gameWorld) -> {
            if (world instanceof ServerWorld serverWorld) {
                AvariciousPayoutComponent.KEY.get(serverWorld).reset();
            }
        });
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
            if (gameWorld.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE
                    || !(gameWorld.getGameMode() instanceof MurderGameMode)) {
                continue;
            }
            tickWorld(world, gameWorld);
        }
    }

    private static void tickWorld(ServerWorld world, GameWorldComponent gameWorld) {
        AvariciousPayoutComponent payout = AvariciousPayoutComponent.KEY.get(world);
        boolean hasAliveAvaricious = world.getPlayers(player ->
                gameWorld.isRole(player, NoellesRoleRegistry.AVARICIOUS)
                        && AvariciousConstants.isEligibleParticipant(player)
        ).size() > 0;

        if (!hasAliveAvaricious) {
            // 没有活着的扒手时不保留旧计时；下一次转职后重新等待完整周期。
            if (payout.hasTimerStartTime()) {
                payout.reset();
            }
            return;
        }

        int currentTime = GameTimeComponent.KEY.get(world).getTime();
        if (!payout.hasTimerStartTime()) {
            // 角色刚分配/转职后的第一个服务端 tick，建立新的完整周期。
            payout.setTimerStartTime(currentTime);
            return;
        }

        int elapsed = payout.getTimerStartTime() - currentTime;
        if (elapsed < 0) {
            // 时停者回溯或其它时间调整让剩余时间增加时，重新校准避免倒计时卡死。
            payout.setTimerStartTime(currentTime);
            return;
        }
        if (elapsed < AvariciousConstants.TIMER_TICKS) {
            return;
        }

        for (ServerPlayerEntity player : world.getPlayers(candidate ->
                gameWorld.isRole(candidate, NoellesRoleRegistry.AVARICIOUS)
                        && AvariciousConstants.isEligibleParticipant(candidate)
        )) {
            int nearbyPlayers = 0;
            for (ServerPlayerEntity other : world.getPlayers()) {
                if (other == player || !AvariciousConstants.isEligibleParticipant(other)) {
                    continue;
                }
                if (other.distanceTo(player) <= AvariciousConstants.MAX_DISTANCE) {
                    nearbyPlayers++;
                }
            }

            if (nearbyPlayers <= 0) {
                continue;
            }

            int stolenAmount = nearbyPlayers * AvariciousConstants.PAYOUT_PER_PLAYER;
            // 保持扒手原有行为：按附近人数给扒手发钱，不扣除其它玩家余额。
            PlayerShopComponent.KEY.get(player).addToBalance(stolenAmount);
            player.playSoundToPlayer(WatheSounds.UI_SHOP_BUY, SoundCategory.PLAYERS, 10.0F, 0.5F);

            NbtCompound extra = new NbtCompound();
            extra.putInt("amount", stolenAmount);
            GameRecordManager.recordGlobalEvent(world, NoellesEventIds.AVARICIOUS_STOLE_COINS_EVENT, player, extra);
        }

        // 无论本次附近是否有人，都推进到下一完整周期，避免每 tick 重复结算。
        payout.setTimerStartTime(currentTime);
    }
}
