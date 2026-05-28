package org.agmas.noellesroles.roles.swapper;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.swapper.SwapperC2SPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class SwapperAbility {

    /**
     * 交换生效的最小延迟秒数。
     * 如果你后续想继续调整交换者强度，优先修改这里。
     */
    public static final int SWAP_DELAY_MIN_SECONDS = 0;

    /**
     * 交换生效的最大延迟秒数。
     * 如果你后续想继续调整交换者强度，优先修改这里。
     */
    public static final int SWAP_DELAY_MAX_SECONDS = 3;

    /**
     * 记录所有“已经发动，但还没真正执行”的交换任务。
     * 这里用交换者自身 UUID 作为 key，目的是避免同一名交换者在同一次技能结算前重复塞入任务。
     */
    private static final Map<UUID, PendingSwap> PENDING_SWAPS = new HashMap<>();

    private SwapperAbility() {
        // 工具类，禁止实例化
    }

    /**
     * 处理交换者的技能请求
     * @param payload 客户端发送的数据包
     * @param player  技能使用玩家（交换者）
     */
    public static void handle(SwapperC2SPacket payload, ServerPlayerEntity player) {
        var world = player.getWorld();
        var gameWorld = GameWorldComponent.KEY.get(world);
        var ability = AbilityPlayerComponent.KEY.get(player);

        // 检查角色
        if (!gameWorld.isRole(player, Noellesroles.SWAPPER)) return;

        // 服务端也要校验冷却，不能只依赖客户端按钮限制，否则恶意发包可以绕过冷却。
        if (ability.cooldown > 0) return;

        // 如果这名交换者已经有一笔待结算的交换任务，就不再重复创建新的任务。
        if (PENDING_SWAPS.containsKey(player.getUuid())) return;

        // 获取两个目标玩家
        PlayerEntity player1 = world.getPlayerByUuid(payload.player());
        PlayerEntity player2 = world.getPlayerByUuid(payload.player2());

        // 先做一次基础校验。真正执行交换时还会再次校验一遍，防止延迟期间目标状态发生变化。
        if (!canSwap(player1, player2)) return;

        // 不再立即交换，而是加入待执行队列，等随机延迟结束后再真正生效。
        int delayTicks = getRandomDelayTicks(player);
        PENDING_SWAPS.put(player.getUuid(), new PendingSwap(player.getUuid(), player1.getUuid(), player2.getUuid(), delayTicks));

        NbtCompound extra = new NbtCompound();
        extra.putUuid("player_one", player1.getUuid());
        extra.putUuid("player_two", player2.getUuid());
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), Noellesroles.SWAPPER_SWAP_SELECTED_EVENT, player, extra);

        // 技能在“发动”的瞬间就进入冷却，避免玩家在延迟窗口内重复使用。
        ability.setCooldown(GameConstants.getInTicks(1, 0)); // 1分钟
    }

    /**
     * 每个服务器 tick 调用一次。
     * 作用是推进所有待执行交换的倒计时，并在时间到达后真正执行交换。
     */
    public static void tickPendingSwaps(MinecraftServer server) {
        if (PENDING_SWAPS.isEmpty()) return;

        Iterator<Map.Entry<UUID, PendingSwap>> iterator = PENDING_SWAPS.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingSwap pendingSwap = iterator.next().getValue();
            pendingSwap.remainingTicks--;

            // 倒计时还没结束，本 tick 不做别的处理。
            if (pendingSwap.remainingTicks > 0) continue;

            // 先从队列中移除，再执行交换，避免极端情况下重复结算。
            iterator.remove();
            executeSwap(server, pendingSwap);
        }
    }

    /**
     * 在回合结束等场景下清空所有待处理交换，
     * 避免上一局残留的延迟交换跑到下一局才触发。
     */
    public static void clearPendingSwaps() {
        PENDING_SWAPS.clear();
    }

    /**
     * 真正执行交换的位置互换逻辑。
     * 这里会重新获取目标玩家并再次校验，确保延迟期间若有人掉线、换维度或状态失效时不会强制交换。
     */
    private static void executeSwap(MinecraftServer server, PendingSwap pendingSwap) {
        ServerPlayerEntity swapper = server.getPlayerManager().getPlayer(pendingSwap.swapperUuid);
        ServerPlayerEntity player1 = server.getPlayerManager().getPlayer(pendingSwap.player1Uuid);
        ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(pendingSwap.player2Uuid);

        if (!canSwap(player1, player2)) return;

        // 获取交换当下的实时位置，而不是记录发动瞬间的位置。
        // 这样才能实现“延迟后再交换当前站位”的效果。
        Vec3d pos1 = player1.getPos();
        Vec3d pos2 = player2.getPos();

        // 使用刷新位置的方法交换站位，尽量保持与项目原本实现一致，避免引入额外副作用。
        player1.refreshPositionAfterTeleport(pos2.x, pos2.y, pos2.z);
        player2.refreshPositionAfterTeleport(pos1.x, pos1.y, pos1.z);

        if (swapper != null) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("player_one", player1.getUuid());
            extra.putUuid("player_two", player2.getUuid());
            GameRecordManager.recordGlobalEvent(swapper.getServerWorld(), Noellesroles.SWAPPER_SWAP_EXECUTED_EVENT, swapper, extra);
        }
    }

    /**
     * 统一封装交换合法性校验。
     * 发动技能时和真正生效时都复用这套规则，避免两处逻辑不一致。
     */
    private static boolean canSwap(PlayerEntity player1, PlayerEntity player2) {
        if (player1 == null || player2 == null) return false;

        // 同一个玩家不能和自己交换，否则只会白白进入冷却。
        if (player1 == player2) return false;

        // 延迟期间如果两名玩家已经不在同一个世界，就取消这次交换，避免跨世界传送带来异常。
        if (player1.getWorld() != player2.getWorld()) return false;

        // 玩家在乘坐载具时强行换位容易引发位置异常，所以直接取消。
        if (player1.hasVehicle() || player2.hasVehicle()) return false;

        // 保留原本的空间校验逻辑，避免在明显不安全的状态下执行换位。
        return player1.getWorld().isSpaceEmpty(player1) && player2.getWorld().isSpaceEmpty(player2);
    }

    /**
     * 根据上方的最小/最大秒数常量，随机生成本次交换要等待的 tick 数。
     * 这里做了最小值/最大值纠正，即使后续手动改常量时写反了，也不会直接报错。
     */
    private static int getRandomDelayTicks(ServerPlayerEntity player) {
        int minSeconds = Math.min(SWAP_DELAY_MIN_SECONDS, SWAP_DELAY_MAX_SECONDS);
        int maxSeconds = Math.max(SWAP_DELAY_MIN_SECONDS, SWAP_DELAY_MAX_SECONDS);
        int minTicks = GameConstants.getInTicks(0, minSeconds);
        int maxTicks = GameConstants.getInTicks(0, maxSeconds);
        return minTicks + player.getRandom().nextInt(maxTicks - minTicks + 1);
    }

    /**
     * 一笔待执行的交换任务。
     * 这里只保留最必要的数据：两个目标玩家和剩余倒计时。
     */
    private static final class PendingSwap {
        private final UUID swapperUuid;
        private final UUID player1Uuid;
        private final UUID player2Uuid;
        private int remainingTicks;

        private PendingSwap(UUID swapperUuid, UUID player1Uuid, UUID player2Uuid, int remainingTicks) {
            this.swapperUuid = swapperUuid;
            this.player1Uuid = player1Uuid;
            this.player2Uuid = player2Uuid;
            this.remainingTicks = remainingTicks;
        }
    }
}
