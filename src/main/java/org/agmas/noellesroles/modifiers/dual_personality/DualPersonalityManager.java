package org.agmas.noellesroles.modifiers.dual_personality;

import dev.doctor4t.wathe.api.PlayerLifeStateApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.compat.TrainVoicePlugin;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SetCameraEntityS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.noellesroles.config.NoellesRolesConfig;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 双重人格的服务端状态机。
 *
 * <p>这个类负责把“词条”变成真实玩法：普通阶段每 60 秒轮换活跃人格，
 * 休眠人格被服务端维持为特殊存活旁观；活跃人格受到致命伤害时进入双活阶段；
 * 双活结束后再走 Wathe 的正常击杀/结算流程。</p>
 */
public final class DualPersonalityManager {

    /*
     * 双活超时需要真正杀死两个人格。
     * 但我们又在 tryInterceptFatalDeath 中拦截普通致命死亡来开启双活。
     * 这个集合是“本次死亡是我们主动触发的超时死亡”的临时标记，
     * 防止超时死亡再次被拦截成新的双活，形成死循环。
     */
    private static final Set<UUID> FORCE_TIMEOUT_DEATHS = new HashSet<>();
    /*
     * 双活启动瞬间的极短保护名单。
     *
     * 范围/贯穿武器通常会先收集一批“当前仍算存活”的目标，再逐个调用 Wathe 的 killPlayer。
     * 休眠人格因为 PlayerLifeStateApi 的 aliveOverride 会被算进这批目标；如果活跃人格先被处理并开启双活，
     * 同一次手雷/狙击贯穿循环里随后轮到原休眠人格时，组件状态已经变成 doubleActive，普通“休眠保护”
     * 就不会再生效，结果会表现成刚解离就立刻死亡。
     *
     * 这个集合只用于吞掉“触发双活同一 tick 里残留的那次旧伤害”，在 tickServer 开头清空；
     * 它不是双活阶段的持续免死。双活真正开始后，下一 tick 起两个人格仍会按正常玩家被伤害和击杀。
     */
    private static final Set<UUID> DISSOCIATION_GRACE_PLAYERS = new HashSet<>();
    /**
     * 客户端同步上来的“人格切换键显示文本”。
     *
     * <p>这个字符串是按玩家客户端当前绑定实际同步过来的，不再是语言文件里的“功能名称”。
     * 这样 actionbar 才能显示成“按下 U 键”或“按下 1 键”，而不是“按下双重人格切换键键”。</p>
     */
    private static final Map<UUID, String> SWITCH_KEY_LABELS = new HashMap<>();

    private static boolean initialized;

    private DualPersonalityManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        // 服务端每 tick 维护普通轮换、休眠状态压制、双活倒计时。
        ServerTickEvents.END_SERVER_TICK.register(DualPersonalityManager::tickServer);
        // 掉线/重连会影响 active/dormant 的控制权，需要专门修正状态。
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> handleDisconnect(handler.getPlayer()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handleJoin(handler.getPlayer()));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // 休眠人格即使客户端意外发出交互，也在服务端拒绝，避免旁观态点尸体/按钮/实体。
            if (player instanceof ServerPlayerEntity serverPlayer && DualPersonalityActionGuard.isDormant(serverPlayer)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    public static void refreshModifierMaximum(ServerWorld world, List<ServerPlayerEntity> players) {
        /*
         * Harpy 的随机词条池用 MODIFIER_MAX 控制是否可能抽到词条。
         * 双重人格至少需要两个人，同时还支持配置“达到多少参局人数才进池”。
         */
        int minPlayers = Math.max(2, NoellesRolesConfig.HANDLER.instance().dualPersonalityMinPlayerSpawn);
        Harpymodloader.MODIFIER_MAX.put(NoellesModifierRegistry.DUAL_PERSONALITY.identifier(), players.size() >= minPlayers ? 1 : 0);
    }

    public static void requestEarlySwitch(ServerPlayerEntity player) {
        // 客户端 U 键只是一条请求，真正能不能提前切换必须由服务端检查当前状态。
        if (!isActiveRound(player.getWorld())) {
            return;
        }
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.getWorld());
        DualPersonalityComponent.PairState pair = component.getPair(player.getUuid());
        if (pair == null || pair.doubleActive || pair.paused || !pair.isActive(player.getUuid())) {
            return;
        }

        switchPersonalities(player.getServerWorld(), component, pair);
    }

    public static boolean tryInterceptFatalDeath(ServerPlayerEntity victim) {
        /*
         * 这个方法在 Wathe 清除 aliveOverride 之前调用。
         * 普通致命伤害会被取消并转成“双活”；双活超时死亡则通过 FORCE_TIMEOUT_DEATHS 放行。
         */
        if (FORCE_TIMEOUT_DEATHS.contains(victim.getUuid())) {
            return false;
        }
        if (!isActiveRound(victim.getWorld())) {
            return false;
        }

        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(victim.getWorld());
        DualPersonalityComponent.PairState pair = component.getPair(victim.getUuid());
        if (pair == null || pair.doubleActive || !pair.isActive(victim.getUuid())) {
            return false;
        }

        UUID partnerUuid = pair.getPartner(victim.getUuid());
        if (partnerUuid == null || victim.getServer().getPlayerManager().getPlayer(partnerUuid) == null) {
            // 另一人格不在线时不强行开启双活，交给 Wathe 原死亡流程处理。
            return false;
        }

        enterDoubleActive(victim.getServerWorld(), component, pair, victim);
        return true;
    }

    public static boolean tryProtectDormantFatalDeath(ServerPlayerEntity victim) {
        /*
         * 这个方法在 Wathe killPlayer 的 HEAD 处调用，比 AllowPlayerDeath、护甲、尸体和回放都更早。
         *
         * 普通轮换阶段的休眠人格虽然是旁观模式，但它通过 PlayerLifeStateApi 被 Wathe 判定为“玩法存活”，
         * 因此手雷、静音手雷、狙击枪这类按 isPlayerAliveAndSurvival 扫目标的武器会把休眠人格也纳入击杀列表。
         * 休眠人格在双活启动前不应该成为真正死亡对象：这里直接取消这次 killPlayer，并重新压回
         * “特殊存活旁观 + 跟随活跃人格视角”的状态，避免生成尸体、死亡回放、杀手金币或死者语音组副作用。
         */
        if (victim == null || FORCE_TIMEOUT_DEATHS.contains(victim.getUuid()) || !isActiveRound(victim.getWorld())) {
            return false;
        }

        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(victim.getWorld());
        DualPersonalityComponent.PairState pair = component.getPair(victim.getUuid());
        if (pair == null) {
            return false;
        }

        if (pair.isDormant(victim.getUuid())) {
            keepDormantAlive(victim, pair);
            return true;
        }

        if (pair.doubleActive && DISSOCIATION_GRACE_PLAYERS.remove(victim.getUuid())) {
            /*
             * 这里只处理“刚刚被活跃人格的致命伤害解离出来、但仍在同一批范围/贯穿武器循环里被继续结算”的旧伤害。
             * 例如手雷循环先处理活跃人格并开启双活，随后又处理原休眠人格；这次后续 killPlayer 其实来自双活前
             * 已经选中的目标快照，不能让它把刚被解离出来的人格秒杀。名单移除后不会继续保护，保证双活阶段
             * 新发生的刀、枪、爆炸、环境死亡都能按 Wathe 原流程正常杀死玩家。
             */
            ensureActive(victim);
            removeRevolversFromInnocent(victim);
            return true;
        }

        return false;
    }

    public static void restoreDormantVoiceChannelAfterDeath(ServerPlayerEntity victim) {
        if (victim == null || !isActiveRound(victim.getWorld())) {
            return;
        }

        DualPersonalityComponent.PairState pair = DualPersonalityComponent.KEY.get(victim.getWorld()).getPair(victim.getUuid());
        if (pair == null || !pair.isDormant(victim.getUuid())) {
            return;
        }

        /*
         * 普通轮换阶段的休眠人格是“玩法上仍存活的旁观相机”，不是 Wathe 意义上的死者。
         * 现在常规 killPlayer 会在 HEAD 被 tryProtectDormantFatalDeath 取消，不再产生尸体/回放/金币等死亡副作用。
         *
         * 这里仅作为兼容兜底：如果未来某个 mixin 改写了调用顺序，让休眠人格仍然走到 Wathe 死亡流程末尾，
         * 至少先撤销 Simple Voice Chat 的死者频道副作用，避免休眠人格继续错进死亡语音组。
         */
        TrainVoicePlugin.resetPlayer(victim.getUuid());
    }

    public static void onSuccessfulKill(ServerPlayerEntity killer, Entity victim, Identifier deathReason) {
        // 只有双活阶段的匕首击杀加时间，其他死因或普通阶段都不影响倒计时。
        if (!"knife_stab".equals(deathReason.getPath())) {
            return;
        }
        if (!isActiveRound(killer.getWorld())) {
            return;
        }
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(killer.getWorld());
        DualPersonalityComponent.PairState pair = component.getPair(killer.getUuid());
        if (pair == null || !pair.doubleActive || !pair.contains(killer.getUuid())) {
            return;
        }
        if (victim instanceof ServerPlayerEntity serverVictim && GameFunctions.isPlayerAliveAndSurvival(serverVictim)) {
            return;
        }

        pair.doubleActiveTicks += DualPersonalityConstants.DOUBLE_ACTIVE_KILL_BONUS_TICKS;
        component.sync();
    }

    public static boolean shouldBlockRevolverPickup(ServerPlayerEntity player, ItemEntity itemEntity) {
        /*
         * 用户确认“好人阵营不能捡枪”按 Wathe 的 isInnocent 判断。
         * 所以这里不只挡普通乘客，也会挡义警等被 Wathe 归为 innocent 的身份。
         */
        if (player == null || itemEntity == null || !itemEntity.getStack().isOf(WatheItems.REVOLVER)) {
            return false;
        }
        if (!isActiveRound(player.getWorld())) {
            return false;
        }
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.getWorld());
        if (!component.isDoubleActive(player.getUuid())) {
            return false;
        }
        return GameWorldComponent.KEY.get(player.getWorld()).isInnocent(player);
    }

    public static boolean shouldSuppressInnocentRevolverPenalty(
            ServerPlayerEntity shooter,
            PlayerEntity target,
            boolean targetNormallyInnocent
    ) {
        /*
         * Wathe 的左轮惩罚是先问“目标是否 innocent”，再决定是否反噬、掉枪和清空理智值。
         * 双活阶段的好人双重人格已经进入独立杀戮窗口，此时其他好人开枪阻止他，
         * 不应该再被 Wathe 当成“好人误伤好人”惩罚。
         */
        if (shooter == null || target == null || !targetNormallyInnocent) {
            return false;
        }
        if (shooter.getUuid().equals(target.getUuid()) || shooter.getWorld() != target.getWorld()) {
            return false;
        }
        if (!isActiveRound(target.getWorld())) {
            return false;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(target.getWorld());
        if (!gameWorldComponent.isInnocent(shooter)) {
            return false;
        }

        /*
         * 只在“双活中的双重人格目标”上豁免惩罚。
         * 普通轮换阶段仍然保留 Wathe 原本的好人误伤惩罚，避免好人随意枪击休眠/活跃人格。
         */
        return DualPersonalityComponent.KEY.get(target.getWorld()).isDoubleActive(target.getUuid());
    }

    private static void tickServer(MinecraftServer server) {
        DISSOCIATION_GRACE_PLAYERS.clear();
        for (ServerWorld world : server.getWorlds()) {
            tickWorld(world);
        }
    }

    private static void tickWorld(ServerWorld world) {
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(world);
        if (component.getPairs().isEmpty()) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(world);
        if (gameWorldComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE) {
            /*
             * Wathe 的 isRunning() 在 STOPPING 结算阶段也会返回 true。
             * 双重人格的相机、休眠锁和双活倒计时只属于真正的 ACTIVE 局内阶段，
             * 进入结算后就不再维护，防止残留状态继续影响玩家。
             */
            return;
        }

        // tick 中可能移除配对，所以复制一份列表迭代，避免边遍历边修改原列表。
        for (DualPersonalityComponent.PairState pair : new ArrayList<>(component.getPairs())) {
            if (pair.doubleActive) {
                tickDoubleActive(world, component, pair);
            } else {
                tickRotatingPair(world, component, pair);
            }
        }
    }

    private static void tickRotatingPair(ServerWorld world, DualPersonalityComponent component, DualPersonalityComponent.PairState pair) {
        ServerPlayerEntity active = world.getServer().getPlayerManager().getPlayer(pair.active);
        ServerPlayerEntity dormant = world.getServer().getPlayerManager().getPlayer(pair.dormant);

        /*
         * 普通轮换阶段最怕掉线导致控制权卡死。
         * active 掉线时，让原 dormant 临时接管；dormant 掉线时，只暂停倒计时等待回归。
         */
        if (active == null) {
            handleActiveOffline(world, component, pair);
            return;
        }
        if (dormant == null) {
            handleDormantOffline(component, pair, active);
            return;
        }

        ensureActive(active);
        boolean forceCameraSync = pair.initialCameraSyncTicks > 0;
        ensureDormant(dormant, active, forceCameraSync);
        if (forceCameraSync) {
            pair.initialCameraSyncTicks--;
        }
        // 休眠人格每 tick 都压技能/物品冷却，属于服务端防绕过兜底。
        DualPersonalityActionGuard.maintainDormantLock(dormant);

        if (pair.paused || pair.jasonWoundedPaused) {
            /*
             * 暂停时仍维持 active/dormant 的模式和相机，但不继续扣轮换倒计时。
             * jasonWoundedPaused 是杰森重伤倒地造成的独立暂停，不复用掉线 paused，
             * 避免重连逻辑把“倒地暂停”误处理成某个人格离线。
             */
            return;
        }

        if (!pair.initialMessageSent) {
            /*
             * 开局身份提示不再和词条/阵营的开局 actionbar 同步发送。
             * 先让它自己空转 2 秒，再发出“你当前为谁、当前人格状态是什么”的提示，
             * 这样开局时信息不会挤成一团。
             */
            if (pair.initialMessageDelayTicks > 0) {
                pair.initialMessageDelayTicks--;
            }
            if (pair.initialMessageDelayTicks <= 0) {
                sendInitialRoleMessages(active, dormant, pair);
                pair.initialMessageSent = true;
                component.sync();
            }
        }

        pair.switchTicks--;
        sendCountdownWarnings(active, dormant, pair.switchTicks);
        if (pair.switchTicks <= 0) {
            // 倒计时归零自动互换 active/dormant。
            switchPersonalities(world, component, pair);
        } else if (pair.switchTicks % 20 == 0) {
            // 客户端只需要秒级刷新，没必要每 tick 同步完整世界组件。
            component.sync();
        }
    }

    public static void updateSwitchKeyLabel(UUID playerUuid, String keyLabel) {
        if (playerUuid == null) {
            return;
        }

        /*
         * 这里存的不是语言文件里的“按钮名称”，而是客户端当前绑定的实际显示文本。
         * 客户端会在按键改动时重新同步，所以这个缓存既能显示当前按键，也能在服务器端独立发 actionbar 时直接复用。
         */
        String sanitized = keyLabel == null ? "" : keyLabel.trim();
        if (sanitized.isEmpty()) {
            SWITCH_KEY_LABELS.remove(playerUuid);
        } else {
            SWITCH_KEY_LABELS.put(playerUuid, sanitized);
        }
    }

    private static void tickDoubleActive(ServerWorld world, DualPersonalityComponent component, DualPersonalityComponent.PairState pair) {
        ServerPlayerEntity main = world.getServer().getPlayerManager().getPlayer(pair.main);
        ServerPlayerEntity sub = world.getServer().getPlayerManager().getPlayer(pair.sub);

        /*
         * 双活阶段允许被正常伤害提前杀死。
         *
         * 之前这里只要玩家对象还在线，就每 tick 无条件 ensureActive。
         * Wathe 真正击杀后会把玩家切成 spectator；下一 tick 又被这里拉回 adventure，
         * 结果表现成“双活期间谁都杀不死”。所以必须先按 Wathe 的局内存活判定过滤：
         * 已经死亡的双重人格不再恢复模式、不再被相机/工具逻辑拉回局内。
         */
        boolean mainAlive = main != null && GameFunctions.isPlayerAliveAndSurvival(main);
        boolean subAlive = sub != null && GameFunctions.isPlayerAliveAndSurvival(sub);

        // 双活阶段仍存活的人格才恢复为正常可行动玩家，同时持续执行好人禁枪规则。
        if (mainAlive) {
            ensureActive(main);
            removeRevolversFromInnocent(main);
        }
        if (subAlive) {
            ensureActive(sub);
            removeRevolversFromInnocent(sub);
        }

        if (!mainAlive && !subAlive) {
            // 两个人格都已被提前击杀时，双活状态已经没有继续倒计时的对象。
            component.removePair(pair.main);
            return;
        }

        pair.doubleActiveTicks--;
        if (pair.doubleActiveTicks <= 0) {
            // 时间耗尽后只强杀仍然存活的人格；已经提前死亡的人不能再次生成尸体/回放。
            if (mainAlive) {
                forceTimeoutDeath(main);
            }
            if (subAlive) {
                forceTimeoutDeath(sub);
            }
            component.removePair(pair.main);
            return;
        }

        if (pair.doubleActiveTicks % 20 == 0) {
            component.sync();
        }
    }

    private static void switchPersonalities(
            ServerWorld world,
            DualPersonalityComponent component,
            DualPersonalityComponent.PairState pair
    ) {
        // 人格切换只交换当前控制权，不改变 main/sub 的身份方向。
        UUID oldActive = pair.active;
        pair.active = pair.dormant;
        pair.dormant = oldActive;
        pair.switchTicks = DualPersonalityConstants.SWITCH_INTERVAL_TICKS;
        pair.paused = false;
        pair.pauseReason = DualPersonalityComponent.PauseReason.NONE;
        pair.jasonWoundedPaused = false;

        ServerPlayerEntity active = world.getServer().getPlayerManager().getPlayer(pair.active);
        ServerPlayerEntity dormant = world.getServer().getPlayerManager().getPlayer(pair.dormant);
        if (active != null) {
            // 新活跃人格恢复冒险模式和自己的视角。
            ensureActive(active);
        }
        if (dormant != null && active != null) {
            // 新休眠人格进入特殊存活旁观，并把相机固定到活跃人格。
            ensureDormant(dormant, active);
        }

        sendSwitchedMessage(active, true);
        sendSwitchedMessage(dormant, false);
        component.sync();
    }

    private static void enterDoubleActive(
            ServerWorld world,
            DualPersonalityComponent component,
            DualPersonalityComponent.PairState pair,
            ServerPlayerEntity trigger
    ) {
        UUID oldDormant = pair.dormant;
        // 致命伤害触发解离：普通轮换停止，两个人格同时获得行动权和双活倒计时。
        pair.doubleActive = true;
        pair.paused = false;
        pair.pauseReason = DualPersonalityComponent.PauseReason.NONE;
        pair.jasonWoundedPaused = false;
        pair.doubleActiveTicks = DualPersonalityConstants.DOUBLE_ACTIVE_BASE_TICKS;
        DISSOCIATION_GRACE_PLAYERS.add(oldDormant);

        ServerPlayerEntity main = world.getServer().getPlayerManager().getPlayer(pair.main);
        ServerPlayerEntity sub = world.getServer().getPlayerManager().getPlayer(pair.sub);

        if (main != null) {
            activateDoubleActivePlayer(main);
        }
        if (sub != null) {
            activateDoubleActivePlayer(sub);
        }

        sendActionbar(main, Text.translatable("message.noellesroles.dual_personality.dissociated"));
        sendActionbar(sub, Text.translatable("message.noellesroles.dual_personality.dissociated"));

        GameRecordManager.recordGlobalEvent(world, NoellesEventIds.DUAL_ACTIVE_STARTED_EVENT, trigger, null);
        component.sync();
    }

    private static void activateDoubleActivePlayer(ServerPlayerEntity player) {
        // 双活启动时保证基础杀戮工具存在；如果背包已有同类物品就不重复塞。
        ensureActive(player);
        giveIfMissing(player, WatheItems.KNIFE);
        giveIfMissing(player, WatheItems.CROWBAR);
        removeRevolversFromInnocent(player);
    }

    private static void giveIfMissing(ServerPlayerEntity player, Item item) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                return;
            }
        }
        player.giveItemStack(item.getDefaultStack());
    }

    private static void removeRevolversFromInnocent(ServerPlayerEntity player) {
        if (!GameWorldComponent.KEY.get(player.getWorld()).isInnocent(player)) {
            return;
        }
        // 双活时如果好人阵营已经持有左轮，也立即掉到地上，保持“不能持枪”的规则一致。
        removeRevolversFromList(player, player.getInventory().main);
        removeRevolversFromList(player, player.getInventory().offHand);
    }

    private static void removeRevolversFromList(ServerPlayerEntity player, List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isOf(WatheItems.REVOLVER)) {
                continue;
            }
            player.dropItem(stack.copy(), true, false);
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    private static void ensureActive(ServerPlayerEntity player) {
        /*
         * changeGameModeAsGameplayAlive 会同时处理 Wathe 的“特殊存活”状态。
         * 只有模式或 aliveOverride 不对时才调用，减少每 tick 重复发包和状态抖动。
         */
        if (player.interactionManager.getGameMode() != GameMode.ADVENTURE || PlayerLifeStateApi.hasAliveOverride(player)) {
            PlayerLifeStateApi.changeGameModeAsGameplayAlive(player, GameMode.ADVENTURE);
        }
        player.setCameraEntity(player);
    }

    private static void ensureDormant(ServerPlayerEntity dormant, ServerPlayerEntity active) {
        ensureDormant(dormant, active, false);
    }

    private static void ensureDormant(ServerPlayerEntity dormant, ServerPlayerEntity active, boolean forceCameraPacket) {
        /*
         * 休眠人格不是死亡，而是“Wathe 仍认为存活”的旁观。
         * 这样胜利判定仍能把他算作活着的人格，但实际操作权被旁观模式和 ActionGuard 锁住。
         */
        if (dormant.interactionManager.getGameMode() != GameMode.SPECTATOR || !PlayerLifeStateApi.hasAliveOverride(dormant)) {
            PlayerLifeStateApi.changeGameModeAsGameplayAlive(dormant, GameMode.SPECTATOR);
        }
        Entity previousCamera = dormant.getCameraEntity();
        dormant.setCameraEntity(active);
        if (forceCameraPacket && previousCamera == active && dormant.getCameraEntity() == active) {
            /*
             * ServerPlayerEntity#setCameraEntity 只有在服务端相机目标发生变化时才会发包。
             * 开局第一轮 tick 里，客户端偶尔还没完全追踪到主人格实体，第一次附身包会被客户端错过；
             * 随后服务端又认为“相机已经是 active”，不会再自动重发，只剩下面的 teleport 让副人格像被强制拉过去。
             *
             * 因此新配对创建后的短窗口内，如果服务端目标已经是 active，就主动补发相机包。
             * 后续人格切换会改变相机目标，原版 setCameraEntity 自己会正常发包，不需要走这个补偿。
             */
            dormant.networkHandler.sendPacket(new SetCameraEntityS2CPacket(active));
            dormant.networkHandler.syncWithPlayerPosition();
        }
        dormant.teleport(active.getX(), active.getY(), active.getZ(), false);
    }

    private static void keepDormantAlive(ServerPlayerEntity dormant, DualPersonalityComponent.PairState pair) {
        ServerPlayerEntity active = dormant.getServer().getPlayerManager().getPlayer(pair.active);
        if (active != null) {
            ensureDormant(dormant, active);
        } else {
            /*
             * 极端时序下活跃人格可能刚掉线、但 tick/断线回调还没来得及把控制权交给另一方。
             * 这时仍然先保住休眠人格的 aliveOverride，并暂时把相机放回自己；随后正常 tick 会进入
             * handleActiveOffline，把在线的一方提为活跃人格或暂停轮换。
             */
            if (dormant.interactionManager.getGameMode() != GameMode.SPECTATOR || !PlayerLifeStateApi.hasAliveOverride(dormant)) {
                PlayerLifeStateApi.changeGameModeAsGameplayAlive(dormant, GameMode.SPECTATOR);
            }
            dormant.setCameraEntity(dormant);
        }

        if (dormant.getHealth() <= 0.0F) {
            // 某些扩展可能先把原版血量扣到 0 再委托 Wathe killPlayer；取消死亡后要避免客户端残留倒地状态。
            dormant.setHealth(1.0F);
        }
        DualPersonalityActionGuard.maintainDormantLock(dormant);
        TrainVoicePlugin.resetPlayer(dormant.getUuid());
    }

    private static void handleDormantOffline(
            DualPersonalityComponent component,
            DualPersonalityComponent.PairState pair,
            ServerPlayerEntity active
    ) {
        // 休眠人格掉线不改变当前控制权，只暂停倒计时，避免在线的活跃人格被切到离线玩家身上。
        if (pair.paused && pair.pauseReason == DualPersonalityComponent.PauseReason.DORMANT_OFFLINE) {
            return;
        }
        pair.paused = true;
        pair.pauseReason = DualPersonalityComponent.PauseReason.DORMANT_OFFLINE;
        sendActionbar(active, Text.translatable(
                "message.noellesroles.dual_personality.dormant_left",
                Text.literal(nameOf(active.getServer(), pair.dormant))
        ));
        component.sync();
    }

    private static void handleActiveOffline(
            ServerWorld world,
            DualPersonalityComponent component,
            DualPersonalityComponent.PairState pair
    ) {
        /*
         * 活跃人格掉线时，如果休眠人格在线，就立即把休眠人格提为活跃人格。
         * 同时暂停倒计时，等原活跃人格回来后作为休眠人格继续这一组关系。
         */
        ServerPlayerEntity oldDormant = world.getServer().getPlayerManager().getPlayer(pair.dormant);
        if (oldDormant == null) {
            pair.paused = true;
            pair.pauseReason = DualPersonalityComponent.PauseReason.ACTIVE_OFFLINE;
            component.sync();
            return;
        }
        if (pair.paused && pair.pauseReason == DualPersonalityComponent.PauseReason.ACTIVE_OFFLINE && pair.isActive(oldDormant.getUuid())) {
            return;
        }

        UUID disconnectedActive = pair.active;
        pair.active = oldDormant.getUuid();
        pair.dormant = disconnectedActive;
        pair.switchTicks = DualPersonalityConstants.SWITCH_INTERVAL_TICKS;
        pair.paused = true;
        pair.pauseReason = DualPersonalityComponent.PauseReason.ACTIVE_OFFLINE;

        ensureActive(oldDormant);
        sendActionbar(oldDormant, Text.translatable(
                "message.noellesroles.dual_personality.active_left",
                Text.literal(nameOf(world.getServer(), disconnectedActive))
        ));
        component.sync();
    }

    private static void handleDisconnect(ServerPlayerEntity player) {
        // Fabric 的断线事件比 tick 更早发现玩家离开，可以更快地修正相机/控制权。
        SWITCH_KEY_LABELS.remove(player.getUuid());
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.getWorld());
        DualPersonalityComponent.PairState pair = component.getPair(player.getUuid());
        if (pair == null || pair.doubleActive) {
            return;
        }

        if (pair.isDormant(player.getUuid())) {
            ServerPlayerEntity active = player.getServer().getPlayerManager().getPlayer(pair.active);
            if (active != null) {
                handleDormantOffline(component, pair, active);
            }
        } else if (pair.isActive(player.getUuid())) {
            handleActiveOffline(player.getServerWorld(), component, pair);
        }
    }

    private static void handleJoin(ServerPlayerEntity player) {
        // 重连后根据暂停原因恢复普通轮换，并重新把休眠人格相机锁到活跃人格。
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.getWorld());
        DualPersonalityComponent.PairState pair = component.getPair(player.getUuid());
        if (pair == null || pair.doubleActive) {
            return;
        }

        ServerPlayerEntity active = player.getServer().getPlayerManager().getPlayer(pair.active);
        ServerPlayerEntity dormant = player.getServer().getPlayerManager().getPlayer(pair.dormant);
        if (active == null || dormant == null) {
            return;
        }

        if (pair.paused && pair.pauseReason == DualPersonalityComponent.PauseReason.DORMANT_OFFLINE && pair.isDormant(player.getUuid())) {
            pair.paused = false;
            pair.pauseReason = DualPersonalityComponent.PauseReason.NONE;
            sendActionbar(active, Text.translatable("message.noellesroles.dual_personality.dormant_returned", player.getDisplayName()));
            sendActionbar(dormant, Text.translatable("message.noellesroles.dual_personality.dormant_returned", player.getDisplayName()));
        } else if (pair.paused && pair.pauseReason == DualPersonalityComponent.PauseReason.ACTIVE_OFFLINE && pair.isDormant(player.getUuid())) {
            pair.paused = false;
            pair.pauseReason = DualPersonalityComponent.PauseReason.NONE;
            pair.switchTicks = DualPersonalityConstants.SWITCH_INTERVAL_TICKS;
            sendActionbar(active, Text.translatable("message.noellesroles.dual_personality.active_returned", player.getDisplayName()));
            sendActionbar(dormant, Text.translatable("message.noellesroles.dual_personality.active_returned", player.getDisplayName()));
        }

        ensureDormant(dormant, active);
        component.sync();
    }

    private static void forceTimeoutDeath(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        // 临时标记本次 killPlayer 为“双活超时强制死亡”，让死亡拦截器放行 Wathe 原流程。
        FORCE_TIMEOUT_DEATHS.add(player.getUuid());
        try {
            GameFunctions.killPlayer(player, true, null, NoellesDeathReasons.DUAL_ACTIVE_TIMEOUT_DEATH_REASON, new NbtCompound());
        } finally {
            FORCE_TIMEOUT_DEATHS.remove(player.getUuid());
        }
    }

    private static void sendInitialRoleMessages(
            ServerPlayerEntity active,
            ServerPlayerEntity dormant,
            DualPersonalityComponent.PairState pair
    ) {
        sendActionbar(active, Text.translatable(
                "message.noellesroles.dual_personality.initial_state",
                Text.translatable(pair.isMain(active.getUuid()) ? "text.noellesroles.dual_personality.main" : "text.noellesroles.dual_personality.sub"),
                Text.translatable("text.noellesroles.dual_personality.active")
        ));
        sendActionbar(dormant, Text.translatable(
                "message.noellesroles.dual_personality.initial_state",
                Text.translatable(pair.isMain(dormant.getUuid()) ? "text.noellesroles.dual_personality.main" : "text.noellesroles.dual_personality.sub"),
                Text.translatable("text.noellesroles.dual_personality.dormant")
        ));
    }

    private static Text getSwitchKeyLabelText(ServerPlayerEntity player) {
        String keyLabel = player == null ? null : SWITCH_KEY_LABELS.get(player.getUuid());
        if (keyLabel == null || keyLabel.isBlank()) {
            keyLabel = "U";
        }
        return Text.literal(keyLabel);
    }

    private static void sendCountdownWarnings(ServerPlayerEntity active, ServerPlayerEntity dormant, int ticksLeft) {
        int secondsLeft = ticksLeft / 20;
        if (ticksLeft == 30 * 20 || ticksLeft == 15 * 20 || ticksLeft == 8 * 20) {
            // actionbar 只在关键秒数提示，避免覆盖玩家其它提示过于频繁。
            sendActionbar(active, Text.translatable(
                    "message.noellesroles.dual_personality.switch_countdown",
                    secondsLeft,
                    Text.translatable("text.noellesroles.dual_personality.can"),
                    getSwitchKeyLabelText(active)
            ));
            sendActionbar(dormant, Text.translatable(
                    "message.noellesroles.dual_personality.switch_countdown",
                    secondsLeft,
                    Text.translatable("text.noellesroles.dual_personality.cannot"),
                    getSwitchKeyLabelText(dormant)
            ));
        } else if (ticksLeft == 3 * 20) {
            sendActionbar(active, Text.translatable("message.noellesroles.dual_personality.switch_soon"));
            sendActionbar(dormant, Text.translatable("message.noellesroles.dual_personality.switch_soon"));
        }
    }

    private static void sendSwitchedMessage(ServerPlayerEntity player, boolean active) {
        if (player == null) {
            return;
        }
        sendActionbar(player, Text.translatable(
                "message.noellesroles.dual_personality.switched",
                Text.translatable(active ? "text.noellesroles.dual_personality.active" : "text.noellesroles.dual_personality.dormant"),
                Text.translatable(active ? "text.noellesroles.dual_personality.can" : "text.noellesroles.dual_personality.cannot"),
                getSwitchKeyLabelText(player)
        ));
    }

    public static void sendActionbar(ServerPlayerEntity player, Text message) {
        if (player == null) {
            return;
        }
        // 统一染成双重人格颜色，便于玩家和其它角色提示区分。
        MutableText colored = Text.empty().append(message).withColor(DualPersonalityConstants.COLOR);
        player.sendMessage(colored, true);
    }

    public static void clearRoundState(ServerWorld world) {
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(world);
        if (component.getPairs().isEmpty()) {
            FORCE_TIMEOUT_DEATHS.clear();
            SWITCH_KEY_LABELS.clear();
            return;
        }

        /*
         * 这里用于“最终离开本局”时清掉双重人格运行态。
         * 注意不要在 stopGame 刚进入 STOPPING 时调用：
         * 结算黑幕/死亡展示阶段仍需要主副人格关系来维持副人格的主人格外观和准星名字。
         * 等 Wathe finalizeGame 把玩家传回准备大厅后再清，才不会让结算画面提前露馅。
         */
        for (DualPersonalityComponent.PairState pair : new ArrayList<>(component.getPairs())) {
            releaseCamera(world, pair.main);
            releaseCamera(world, pair.sub);
        }
        component.clear();
        FORCE_TIMEOUT_DEATHS.clear();
        SWITCH_KEY_LABELS.clear();
    }

    public static boolean isActiveRound(World world) {
        return world != null
                && GameWorldComponent.KEY.get(world).getGameStatus() == GameWorldComponent.GameStatus.ACTIVE;
    }

    private static void releaseCamera(ServerWorld world, UUID playerUuid) {
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerUuid);
        if (player != null) {
            player.setCameraEntity(player);
        }
    }

    private static String nameOf(MinecraftServer server, UUID uuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return player.getNameForScoreboard();
        }
        return ForcedDualPersonalityManager.describePlayer(uuid);
    }
}
