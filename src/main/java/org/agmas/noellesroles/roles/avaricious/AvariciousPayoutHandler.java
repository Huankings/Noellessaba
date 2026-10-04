package org.agmas.noellesroles.roles.avaricious;

import dev.doctor4t.wathe.api.event.GameEvents;
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
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWorldComponent;

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
        /*
         * 回溯期间由 TimekeeperWorldComponent 逐帧恢复历史世界快照。
         * 此时不能再按“当前现实 tick”递减扒手计时，否则会在历史帧之外额外消耗冷却；
         * 等回溯结束后，计时会从最终恢复的历史值继续正常推进。
         */
        if (TimekeeperWorldComponent.KEY.get(world).isRewinding()) {
            return;
        }

        AvariciousPayoutComponent payout = AvariciousPayoutComponent.KEY.get(world);
        boolean hasAvariciousRole = world.getPlayers(player ->
                gameWorld.isRole(player, NoellesRoleRegistry.AVARICIOUS)
        ).size() > 0;
        boolean hasEligibleAvaricious = world.getPlayers(player ->
                gameWorld.isRole(player, NoellesRoleRegistry.AVARICIOUS)
                        && AvariciousConstants.isEligibleParticipant(player)
        ).size() > 0;

        if (!hasEligibleAvaricious) {
            /*
             * 被杀死的扒手或处于时间狭缝的扒手都不能继续触发偷钱。
             * 职业仍存在时保留当前剩余 tick，只是暂停推进；这样狭缝结束或回溯恢复后，
             * 倒计时不会因为一次临时非存活状态被额外消耗。职业已经被移除时才清空旧计时，
             * 防止下一次重新分配扒手直接继承上一轮周期。
             */
            if (!hasAvariciousRole && payout.hasTimer()) {
                payout.reset();
            }
            return;
        }

        if (!payout.hasTimer()) {
            // 角色刚分配/转职后的第一个服务端 tick，建立新的独立完整周期。
            payout.startTimer();
            return;
        }

        /*
         * 这里推进的是扒手自己的剩余 tick，而不是 Wathe 的 GameTimeComponent.time。
         * 因此 Wathe 加时/减时不会改变偷钱周期；时停者回溯则会通过
         * TimekeeperSnapshots 恢复该世界组件的历史值，仍保持原有回溯语义。
         */
        if (!payout.tickTimer()) {
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
        payout.startTimer();
    }
}
