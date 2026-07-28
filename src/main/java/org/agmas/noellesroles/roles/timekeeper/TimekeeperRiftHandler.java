package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityConstants;
import org.agmas.noellesroles.modifiers.lovers.LoversConstants;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.arsonist.ArsonistConstants;
import org.agmas.noellesroles.roles.thief.ThiefItemTracker;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 时间狭缝入口判断。
 *
 * <p>狭缝不是普通死亡保护，它必须等 Wathe 的死亡流程真正完成后再介入：
 * 先让原本的死亡、尸体、掉落物、回放照常成立，
 * 再把死者临时拉回“特殊存活旁观”。
 * 这样 30 秒内若被时间回溯覆盖，快照系统可以把这名玩家还原成存活态；
 * 若没有被回溯覆盖，则狭缝倒计时结束后再回到普通死亡旁观。</p>
 */
public final class TimekeeperRiftHandler {
    private TimekeeperRiftHandler() {
    }

    public static boolean shouldSuppressRepeatedDeathInRift(@NotNull ServerPlayerEntity victim) {
        /*
         * 时间狭缝里的玩家已经完成过一次真正死亡流程：
         * Wathe 已经记录死亡、生成尸体/掉落物、增加无辜死亡时间、切换语音频道，
         * 随后我们才把他临时拉回“特殊存活旁观”，等待 30 秒内可能发生的时间回溯。
         *
         * 这个特殊存活旁观会让 GameFunctions.isPlayerAliveAndSurvival(...) 继续返回 true。
         * 因此像“跌出列车边界”这种每 tick 扫描存活玩家的环境死因，会不断把狭缝玩家重新送进
         * GameFunctions.killPlayer(...)。如果不在死亡流程开头吞掉，后续会反复产生尸体、回放、
         * 击杀奖励、无辜死亡加时和语音频道变动，严重时还会造成卡顿。
         *
         * 注意这里不能用“当前是否仍被 Wathe 视作存活”作为条件：
         * 狭缝状态本身就是靠 aliveOverride 维持的；只要 TimekeeperPlayerComponent 还标记为
         * inTimeRift，第二次及之后的 killPlayer 都只是同一次死亡状态的重复触发。
         */
        return TimekeeperPlayerComponent.KEY.get(victim).isInTimeRift();
    }

    public static void tryStartRiftAfterDeath(@NotNull ServerPlayerEntity victim) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(victim.getServerWorld());
        TimekeeperWorldComponent worldComponent = TimekeeperWorldComponent.KEY.get(victim.getServerWorld());

        /*
         * 回溯播放期间正在连续应用历史快照。
         * 此时再把某个“播放中瞬间死亡”的玩家塞进狭缝，会制造出一条和目标快照不一致的新状态。
         * 因此狭缝只在正常时间线运行时触发。
         */
        if (!gameWorld.isRunning() || worldComponent.isRewinding()) {
            return;
        }

        if (GameFunctions.isPlayerAliveAndSurvival(victim) || TimekeeperPlayerComponent.KEY.get(victim).isInTimeRift()) {
            return;
        }

        /*
         * 需求写的是“时停者存活且背包内怀表未损坏时，其他玩家死亡后进入时间狭缝”。
         * 因此时停者本人死亡不会用自己的机制把自己塞进狭缝；
         * 同时怀表必须在背包里且不是损坏状态，精致怀表也算有效。
         */
        if (gameWorld.isRole(victim, NoellesRoleRegistry.TIMEKEEPER)) {
            return;
        }

        if (!hasAliveTimekeeperWithUsableWatch(victim)) {
            return;
        }

        TimekeeperPlayerComponent.KEY.get(victim).startTimeRift();
        startLoverPartnerRiftAfterDeath(victim);

        /*
         * 死亡事件可能发生在 Wathe 胜利 tick 之前，也可能发生在之后。
         * 这里启动狭缝后立刻跑一次全局收束检查：如果这名死者正好是最后一个杀手，
         * 或者是最后一个阻拦普通胜利的独立职业，就不等 30 秒倒计时，马上把他/她转回真死亡旁观，
         * 让本 tick 或下一 tick 的胜利仲裁可以正常结算。
         */
        tickActiveRifts(victim.getServerWorld());
    }

    /**
     * 每 tick 检查所有时间狭缝是否仍然有存在理由。
     *
     * <p>时间狭缝的语义是“时停者仍活着且仍持有可用怀表时，给 30 秒回溯窗口”。
     * 因此它不是一次性死亡保护：如果时停者在窗口内死亡、怀表损坏/丢失，
     * 或者狭缝中的死者已经成为阻塞胜利结算的唯一原因，就必须马上退出狭缝，
     * 清掉 Wathe 的特殊存活授权并回到普通死亡频道。</p>
     */
    public static void tickActiveRifts(@NotNull ServerWorld world) {
        if (!hasActiveRift(world)) {
            return;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        if (!gameWorld.isRunning()) {
            finishAllActiveRifts(world);
            return;
        }

        /*
         * 回溯播放期间每 tick 都在恢复历史快照，玩家的生死、背包和组件状态都是“正在倒放”的临时画面。
         * 此时不做狭缝胜利收束，避免刚恢复到某个历史帧就把狭缝提前关闭，破坏回溯抵达点的状态。
         */
        if (TimekeeperWorldComponent.KEY.get(world).isRewinding()) {
            return;
        }

        if (!hasAliveTimekeeperWithUsableWatch(world)) {
            finishAllActiveRifts(world);
            return;
        }

        if (wouldGameEndWithoutActiveRifts(world, gameWorld)) {
            finishAllActiveRifts(world);
        }
    }

    public static boolean hasAliveTimekeeperWithUsableWatch(@NotNull ServerWorld world) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!gameWorld.isRole(player, NoellesRoleRegistry.TIMEKEEPER)
                    || !GameFunctions.isPlayerAliveAndSurvival(player)) {
                continue;
            }

            for (int slot = 0; slot < player.getInventory().size(); slot++) {
                if (TimekeeperWatchItem.isUsableWatch(player.getInventory().getStack(slot))
                        && player.getInventory().getStack(slot).isOf(ModItems.DYING_WATCH)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAliveTimekeeperWithUsableWatch(@NotNull ServerPlayerEntity victim) {
        return hasAliveTimekeeperWithUsableWatch(victim.getServerWorld());
    }

    private static boolean hasActiveRift(@NotNull ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()) {
                return true;
            }
        }
        return false;
    }

    private static void startLoverPartnerRiftAfterDeath(@NotNull ServerPlayerEntity victim) {
        ServerWorld world = victim.getServerWorld();
        WorldModifierComponent modifiers = WorldModifierComponent.KEY.get(world);
        if (!modifiers.isModifier(victim, NoellesModifierRegistry.LOVERS)) {
            return;
        }

        UUID partnerUuid = getLoverPartnerUuid(victim, modifiers);
        if (partnerUuid == null) {
            return;
        }

        ServerPlayerEntity partner = world.getServer().getPlayerManager().getPlayer(partnerUuid);
        if (partner == null
                || partner.getUuid().equals(victim.getUuid())
                || TimekeeperPlayerComponent.KEY.get(partner).isInTimeRift()
                || !GameFunctions.isPlayerAliveAndSurvival(partner)) {
            return;
        }

        /*
         * 恋人殉情原本放在 LoversVictoryRule 里，依赖 VictoryApi 传入的 alivePlayers。
         * 但时间狭缝会把死者临时标记成“特殊存活旁观”，导致 VictoryApi 仍把已经死亡的恋人
         * 当成存活玩家：A 死亡进入狭缝后，B 会因为“伴侣仍在 alivePlayers 里”而暂时不殉情，
         * 直到 A 的狭缝结束才被延迟杀死。
         *
         * 这里改在“死亡确认并成功进入狭缝”的瞬间处理伴侣：
         * 1. A 进入狭缝后，立刻用 broken_heart 死因杀死仍真正存活的伴侣 B；
         * 2. B 的死亡会再次走 Wathe killPlayer，随后由 TimekeeperDeathRiftMixin 把 B 也拉进狭缝；
         * 3. B 进入狭缝时会再次检查伴侣 A，但 A 已经 isInTimeRift()，所以上面的保护会直接 return，
         *    不会形成 A -> B -> A 的递归死亡循环。
         *
         * 仍然走 GameFunctions.killPlayer，而不是直接 startTimeRift()，是为了保留尸体、掉落物、回放、
         * 死亡语音和其他死亡链处理；时间回溯的快照系统随后可以同时把两人的死亡状态回滚回来。
         */
        NbtCompound extraDeathData = new NbtCompound();
        extraDeathData.putUuid("broken_heart_partner", victim.getUuid());
        GameFunctions.killPlayer(partner, true, null, NoellesDeathReasons.BROKEN_HEART_DEATH_REASON, extraDeathData);
    }

    private static UUID getLoverPartnerUuid(
            @NotNull ServerPlayerEntity victim,
            @NotNull WorldModifierComponent modifiers
    ) {
        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(victim.getServerWorld());
        UUID partnerUuid = pairComponent.getPartner(victim.getUuid());
        if (partnerUuid != null) {
            return partnerUuid;
        }

        /*
         * 兼容旧数据或调试状态：如果没有显式 pair，但本局刚好只有两个 LOVERS，
         * LoversPairComponent 可以按旧版语义把另一名 LOVERS 玩家当作伴侣。
         */
        return pairComponent.getPartnerOrFallback(
                victim.getUuid(),
                modifiers.getAllWithModifier(NoellesModifierRegistry.LOVERS)
        );
    }

    private static void finishAllActiveRifts(@NotNull ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            TimekeeperPlayerComponent component = TimekeeperPlayerComponent.KEY.get(player);
            if (component.isInTimeRift()) {
                component.finishTimeRift();
            }
        }
    }

    private static boolean wouldGameEndWithoutActiveRifts(@NotNull ServerWorld world, @NotNull GameWorldComponent gameWorld) {
        GameFunctions.WinStatus vanillaWinStatus = calculateVanillaWinStatusIgnoringRifts(world, gameWorld);
        if (vanillaWinStatus == GameFunctions.WinStatus.NONE) {
            return false;
        }

        /*
         * 这里做的是“排除狭缝后的胜利预演”：
         * 1. vanillaWinStatus 是 Wathe 普通杀手/乘客/时间胜利在“狭缝玩家算死”的前提下会得到的结果；
         * 2. alivePlayers 只包含非狭缝且仍被 Wathe 视作存活的人；
         * 3. hasKnownNoellesVictoryBlocker 再模拟 NoellesRoles 自己那些会 KEEP_RUNNING 的独立/词条规则。
         *
         * 如果排除狭缝后仍有真正活着的阻拦者，例如一个还活着的纵火犯、召集者、
         * 双重人格或按配置会保活的恋人，就不能提前清掉狭缝；因为游戏本来就还不该结算。
         * 反过来，如果没有这类真实存活阻拦者，就说明当前胜利之所以没发生，只是因为死者
         * 被时间狭缝临时当作“特殊存活旁观”。这时必须把狭缝收束成真死亡，让胜利马上结算。
         */
        List<ServerPlayerEntity> alivePlayers = getAlivePlayersIgnoringRifts(world);
        return !hasKnownNoellesVictoryBlocker(alivePlayers, gameWorld, vanillaWinStatus);
    }

    private static GameFunctions.@NotNull WinStatus calculateVanillaWinStatusIgnoringRifts(
            @NotNull ServerWorld world,
            @NotNull GameWorldComponent gameWorld
    ) {
        if (!GameTimeComponent.KEY.get(world).hasTime()) {
            return GameFunctions.WinStatus.TIME;
        }

        boolean civilianAlive = false;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!isAliveIgnoringRift(player)) {
                continue;
            }
            if (gameWorld.isInnocent(player)) {
                civilianAlive = true;
                break;
            }
        }

        if (!civilianAlive) {
            return GameFunctions.WinStatus.KILLERS;
        }

        for (UUID playerUuid : gameWorld.getAllKillerTeamPlayers()) {
            if (world.getPlayerByUuid(playerUuid) instanceof ServerPlayerEntity player && isAliveIgnoringRift(player)) {
                return GameFunctions.WinStatus.NONE;
            }
        }
        return GameFunctions.WinStatus.PASSENGERS;
    }

    private static @NotNull List<ServerPlayerEntity> getAlivePlayersIgnoringRifts(@NotNull ServerWorld world) {
        return world.getPlayers(TimekeeperRiftHandler::isAliveIgnoringRift);
    }

    private static boolean isAliveIgnoringRift(ServerPlayerEntity player) {
        if (player == null || TimekeeperPlayerComponent.KEY.get(player).isInTimeRift()) {
            return false;
        }
        return GameFunctions.isPlayerAliveAndSurvival(player);
    }

    private static boolean hasKnownNoellesVictoryBlocker(
            @NotNull List<ServerPlayerEntity> alivePlayers,
            @NotNull GameWorldComponent gameWorld,
            GameFunctions.@NotNull WinStatus vanillaWinStatus
    ) {
        if (vanillaWinStatus != GameFunctions.WinStatus.KILLERS
                && vanillaWinStatus != GameFunctions.WinStatus.PASSENGERS) {
            return false;
        }

        /*
         * 只剩一个玩家时，NoellesRoles 的独立阻拦职业会走自己的 custom win，
         * 不会继续 keepRunning；这里不能把“将要独胜的人”误判成仍在拖局。
         */
        if (alivePlayers.size() <= 1) {
            return false;
        }

        if (ArsonistConstants.KEEPS_GAME_GOING && hasAliveRole(alivePlayers, gameWorld, NoellesRoleRegistry.ARSONIST)) {
            return true;
        }
        if (hasAliveRole(alivePlayers, gameWorld, NoellesRoleRegistry.CONVENER)) {
            return true;
        }
        if (hasAliveRole(alivePlayers, gameWorld, NoellesRoleRegistry.LICENSED_VILLAIN)) {
            return true;
        }
        if (ThiefItemTracker.isWeaponAvailable() && hasAliveRole(alivePlayers, gameWorld, NoellesRoleRegistry.THIEF)) {
            return true;
        }

        WorldModifierComponent modifiers = WorldModifierComponent.KEY.get(alivePlayers.get(0).getServerWorld());
        if (hasLoversVictoryBlocker(alivePlayers, gameWorld, modifiers, vanillaWinStatus)) {
            return true;
        }

        /*
         * 双重人格当前配置为不与杀手/乘客共胜。只要排除狭缝后仍有非狭缝双重人格玩家，
         * 且场上并不是“全员双重人格独胜”的局面，VictoryApi 仍会 keepRunning。
         *
         * 例子：
         * - 场上剩一个好人 A、一个双重人格 B、一个杀手 C。
         * - C 被杀死并进入时间狭缝。
         * - 排除 C 后，Wathe 普通逻辑会认为“杀手全灭，乘客胜利”。
         * - 但 B 仍真正存活，并且 DualPersonalityConstants.WIN_WITH_CIVILIANS=false，
         *   所以双重人格规则会拦住乘客胜利继续等待自己独胜。
         * - 因此这里返回 true，表示“还有真实阻拦者”，不能因为 C 在狭缝里就立刻清掉狭缝。
         *
         * 反例：
         * - 如果唯一的双重人格 B 已经死亡并进入狭缝，排除狭缝后场上只剩好人。
         * - 这里的 alivePlayers 里没有 B，于是不会返回 true。
         * - TimekeeperRiftHandler 会 finishTimeRift()，让 B 变成真死亡，乘客胜利可以马上结算。
         */
        long dualPersonalityCount = alivePlayers.stream()
                .filter(player -> modifiers.isModifier(player, NoellesModifierRegistry.DUAL_PERSONALITY))
                .count();
        if (dualPersonalityCount > 0 && dualPersonalityCount < alivePlayers.size()) {
            if (vanillaWinStatus == GameFunctions.WinStatus.KILLERS && !DualPersonalityConstants.WIN_WITH_KILLERS) {
                return true;
            }
            if (vanillaWinStatus == GameFunctions.WinStatus.PASSENGERS && !DualPersonalityConstants.WIN_WITH_CIVILIANS) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasLoversVictoryBlocker(
            @NotNull List<ServerPlayerEntity> alivePlayers,
            @NotNull GameWorldComponent gameWorld,
            @NotNull WorldModifierComponent modifiers,
            GameFunctions.@NotNull WinStatus vanillaWinStatus
    ) {
        List<ServerPlayerEntity> lovers = alivePlayers.stream()
                .filter(player -> modifiers.isModifier(player, NoellesModifierRegistry.LOVERS))
                .toList();
        if (lovers.isEmpty()) {
            return false;
        }

        /*
         * 恋人规则里，“场上活人全是恋人”不是阻拦结算，而是直接进入恋人独立胜利。
         * 这种情况下狭缝不需要继续保留：清掉狭缝后，下一轮 VictoryApi 会按真实存活列表
         * 重新判断恋人独胜或殉情，不应该让已经死亡的狭缝玩家继续支撑一个临时胜利状态。
         */
        if (lovers.size() == alivePlayers.size()) {
            return false;
        }

        /*
         * LoversVictoryRule 的顺序是：
         * 1. 先处理殉情；
         * 2. 再判断“全员恋人”独胜；
         * 3. 再尝试配置允许时的杀手恋人共胜；
         * 4. 最后才用配置决定是否 KEEP_RUNNING 拖住普通杀手/乘客结算。
         *
         * 所以这里也先复刻“杀手恋人共胜”的放行条件。
         * 如果未来 LoversVictoryRule 的配置语义发生变化，这里也必须同步调整，
         * 否则时间狭缝会和真正的 VictoryApi 行为不同步。
         */
        if (vanillaWinStatus == GameFunctions.WinStatus.KILLERS
                && LoversConstants.WIN_WITH_KILLERS
                && canLoversCoWinWithKillers(alivePlayers, lovers, gameWorld)) {
            return false;
        }

        /*
         * 当前恋人保活开关按 LoversVictoryRule 现有代码执行：
         * WIN_WITH_CIVILIANS=false 时，只要仍有非狭缝恋人在场，并且普通阵营准备结算，
         * 恋人就会 KEEP_RUNNING 继续争取自己的独胜窗口。
         *
         * 这也是本方法要补进时间狭缝的重点：如果恋人本人还真正活着并按配置会拖局，
         * 不应该因为另一个死者在狭缝里，就提前把狭缝全部收束。
         */
        return !LoversConstants.WIN_WITH_CIVILIANS
                && (vanillaWinStatus == GameFunctions.WinStatus.KILLERS
                || vanillaWinStatus == GameFunctions.WinStatus.PASSENGERS);
    }

    private static boolean canLoversCoWinWithKillers(
            @NotNull List<ServerPlayerEntity> alivePlayers,
            @NotNull List<ServerPlayerEntity> lovers,
            @NotNull GameWorldComponent gameWorld
    ) {
        List<UUID> loverUuids = lovers.stream().map(ServerPlayerEntity::getUuid).toList();
        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(alivePlayers.get(0).getServerWorld());
        long nonInnocentCount = alivePlayers.stream()
                .filter(player -> !gameWorld.isInnocent(player))
                .count();

        for (ServerPlayerEntity lover : lovers) {
            UUID partnerUuid = pairComponent.getPartnerOrFallback(lover.getUuid(), loverUuids);
            if (partnerUuid == null || !loverUuids.contains(partnerUuid)) {
                continue;
            }

            ServerPlayerEntity partner = findAlivePlayerByUuid(lovers, partnerUuid);
            if (partner == null) {
                continue;
            }

            /*
             * 这段条件和 LoversVictoryRule#tryKillerCoWin 保持一致：
             * 至少一名恋人必须属于非无辜阵营，并且场上除了一个无辜恋人外，
             * 其余存活者都不是无辜阵营，才允许恋人跟随杀手普通胜利共胜。
             */
            if (gameWorld.isInnocent(lover) && gameWorld.isInnocent(partner)) {
                continue;
            }
            if (alivePlayers.size() - 1 == nonInnocentCount) {
                return true;
            }
        }
        return false;
    }

    private static ServerPlayerEntity findAlivePlayerByUuid(
            @NotNull List<ServerPlayerEntity> players,
            @NotNull UUID uuid
    ) {
        for (ServerPlayerEntity player : players) {
            if (player.getUuid().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    private static boolean hasAliveRole(
            @NotNull List<ServerPlayerEntity> alivePlayers,
            @NotNull GameWorldComponent gameWorld,
            @NotNull Role role
    ) {
        for (ServerPlayerEntity player : alivePlayers) {
            if (gameWorld.isRole(player, role)) {
                return true;
            }
        }
        return false;
    }
}
