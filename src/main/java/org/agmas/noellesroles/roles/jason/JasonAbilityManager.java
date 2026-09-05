package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.api.death.DeathContext;
import dev.doctor4t.wathe.api.death.DeathDecision;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import org.agmas.noellesroles.packet.role.jason.JasonAbilitySoundS2CPacket;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 杰森“无恶不在”的服务端权威状态机。
 *
 * <p>这个类只处理会影响玩法结算的事情：能力键切换、阶段推进、冷却、右键封锁、
 * 死亡保护、惊吓结算、回放和音效指令。客户端雾效、红色粒子和输入表现只读取这里同步出的 CCA 状态，
 * 不能反过来决定能力是否真的生效。</p>
 */
public final class JasonAbilityManager {
    private static boolean initialized;

    private JasonAbilityManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(JasonAbilityManager::tickServer);

        /*
         * 服务端右键兜底：无恶不在期间杰森不能使用物品、方块或实体。
         * 客户端会先吞左/右键，但服务端仍必须挡住延迟包、宏或改包客户端。
         * 这里不处理 G 键，因为用户确认无恶不在期间 G 键仍用于主动解除。
         */
        UseItemCallback.EVENT.register((player, world, hand) -> JasonAbilityRules.isAbilityActionLocked(player)
                ? TypedActionResult.fail(player.getStackInHand(hand))
                : TypedActionResult.pass(player.getStackInHand(hand)));
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> JasonAbilityRules.isAbilityActionLocked(player)
                ? ActionResult.FAIL
                : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> JasonAbilityRules.isAbilityActionLocked(player)
                ? ActionResult.FAIL
                : ActionResult.PASS);

        /*
         * 杰森离开游戏时无恶不在必须解除，并立刻停掉所有客户端循环音。
         * 组件随实体卸载不一定能给其他客户端一个“状态已清空”的同步包，所以这里显式发 STOP_LOOP。
         */
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (JasonAbilityPlayerComponent.KEY.get(player).isActiveLike()) {
                broadcastSound(server, JasonAbilitySoundS2CPacket.Action.STOP_LOOP);
                clearWorldBlindness(player.getServerWorld());
            }
            JasonAbilityPlayerComponent.KEY.get(player).forceClear(false);
            JasonAbilityBlindnessComponent.KEY.get(player).clearOwnedEffect();
        });
    }

    /**
     * 处理通用 G 键能力包。
     *
     * <p>进入杰森模式、倒地、非存活或冷却中都会拒绝发动；处于 ACTIVE 且满足 7 秒限制时，
     * 同一枚 G 键会改为请求主动解除。</p>
     */
    public static void handleAbilityKey(@NotNull ServerPlayerEntity player) {
        if (!JasonAbilityRules.isAliveJason(player) || JasonWoundManager.isWoundedActionLocked(player)) {
            return;
        }
        if (JasonPsychoHandler.isJasonModeActive(player)) {
            forceExitForJasonMode(player);
            return;
        }

        JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(player);
        if (component.canUseAbility()) {
            startAbility(player, component);
            return;
        }
        if (component.canRequestExit()) {
            requestExit(player, component);
        }
    }

    /**
     * 杰森模式启动时强制解除无恶不在。
     *
     * <p>这不是“主动解除”，因此不记录主动解除/完全解除回放，也不触发惊吓。
     * 目的只是保证两个强状态不会叠加。</p>
     */
    public static void forceExitForJasonMode(@NotNull ServerPlayerEntity player) {
        forceClearAbility(player, false, true);
    }

    /**
     * 玩家重置或回合清理时清掉无恶不在状态。
     */
    public static void resetPlayer(@NotNull PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            forceClearAbility(serverPlayer, false, true);
        }
        JasonAbilityPlayerComponent.KEY.get(player).reset();
        JasonAbilityBlindnessComponent.KEY.get(player).clearOwnedEffect();
    }

    /**
     * 回合结束时的静态/客户端音效兜底清理。
     *
     * <p>无恶不在没有额外静态集合，但持续音是客户端本地循环，如果回合结束时杰森实体已经被重置或离线，
     * 仅靠组件同步可能漏掉 STOP_LOOP。这里在 finalize 再全局广播一次停止指令。</p>
     */
    public static void resetRoundTransientState(@NotNull ServerWorld world) {
        broadcastSound(world.getServer(), JasonAbilitySoundS2CPacket.Action.STOP_LOOP);
        clearWorldBlindness(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            JasonAbilityPlayerComponent.KEY.get(player).reset();
            JasonAbilityBlindnessComponent.KEY.get(player).clearOwnedEffect();
        }
    }

    /**
     * 击杀成功后清除“解除后的 15 秒冷却”。
     *
     * <p>组件内部会检查冷却类型，确保不会把开局 40 秒冷却错误清掉。</p>
     */
    public static void clearAfterExitCooldownFromKill(@NotNull ServerPlayerEntity player) {
        JasonAbilityPlayerComponent.KEY.get(player).clearAfterExitCooldownFromKill();
    }

    /**
     * 死亡确认前保护幽魂杰森。
     *
     * <p>需求指定：无恶不在状态下，存活玩家造成的死亡不能生效；非存活 / 创造 / 旁观来源作为管理员调试放行。
     * 因此这里只在 killer 是 Wathe 局内存活玩家时取消，并且放行 fell_out_of_train。</p>
     */
    public static @NotNull DeathDecision protectFromSurvivalFatalDamage(@NotNull DeathContext context) {
        if (!(context.victim() instanceof ServerPlayerEntity victim) || !JasonAbilityRules.isAbilityActiveLike(victim)) {
            return DeathDecision.PASS;
        }
        if (GameConstants.DeathReasons.FELL_OUT_OF_TRAIN.equals(context.deathReason())) {
            return DeathDecision.PASS;
        }
        PlayerEntity killer = context.killer();
        if (killer != null && GameFunctions.isPlayerAliveAndSurvival(killer)) {
            return DeathDecision.CANCEL;
        }
        return DeathDecision.PASS;
    }

    /**
     * 死亡流程结束后的清理与冷却刷新。
     */
    public static void handleDeathAttempt(@NotNull DeathContext context) {
        if (context.confirmedDeath()) {
            if (context.victim() instanceof ServerPlayerEntity victim && JasonAbilityPlayerComponent.KEY.get(victim).isActiveLike()) {
                forceClearAbility(victim, false, true);
            }
            if (context.serverKiller() != null && JasonAbilityRules.isAliveJason(context.serverKiller())) {
                clearAfterExitCooldownFromKill(context.serverKiller());
            }
        }
    }

    /**
     * 原版伤害入口的兜底。
     *
     * <p>Wathe 的枪、刀和职业击杀大多走 {@link dev.doctor4t.wathe.game.GameFunctions#killPlayer}，
     * 但窒息、火焰、仙人掌、摔落等原版自然伤害会直接进入 LivingEntity#damage。
     * 用户要求无恶不在期间不吃这些自然伤害，同时保留 /kill 作为管理员测试入口，
     * 因此这里只放行 DamageTypes.GENERIC_KILL，其它普通伤害全部取消。</p>
     */
    public static boolean shouldCancelAbilityDamage(@Nullable PlayerEntity target, @Nullable DamageSource source) {
        if (!JasonAbilityRules.isAbilityActiveLike(target) || source == null) {
            return false;
        }
        clearAbilityFire(target);
        return !source.isOf(DamageTypes.GENERIC_KILL);
    }

    /**
     * 无恶不在期间清理杰森身上的原版火焰状态。
     *
     * <p>火焰既会造成 on_fire / in_fire 伤害，也会在客户端显示燃烧特效。
     * 即使伤害已经在 damage 入口被取消，也要持续把 fireTicks 清零，避免幽魂杰森带着火焰特效暴露位置。</p>
     */
    public static void clearAbilityFire(@Nullable PlayerEntity player) {
        if (player != null && JasonAbilityRules.isAbilityActiveLike(player) && player.getFireTicks() > 0) {
            player.setFireTicks(0);
        }
    }

    /**
     * 惊吓状态是否正在生效。
     */
    public static boolean isScared(@Nullable PlayerEntity player) {
        return player != null
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && JasonAbilityPlayerComponent.KEY.get(player).getScaredTicks() > 0;
    }

    private static void tickServer(@NotNull MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            tickWorld(world);
        }
    }

    private static void tickWorld(@NotNull ServerWorld world) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(world);
        boolean hasActiveJason = world.getPlayers().stream().anyMatch(JasonAbilityRules::isAbilityActiveLike);
        boolean hasExitingJason = world.getPlayers().stream()
                .anyMatch(player -> JasonAbilityRules.isAliveJason(player)
                        && JasonAbilityPlayerComponent.KEY.get(player).isExiting());
        for (ServerPlayerEntity player : world.getPlayers()) {
            JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(player);
            updateBlindnessEffect(player, hasActiveJason, hasExitingJason);
            tickScare(player, component);

            if (!GameFunctions.isPlayerAliveAndSurvival(player) || !gameWorld.isRole(player, NoellesRoleRegistry.JASON)) {
                if (component.isActiveLike() || component.getCooldownTicks() > 0) {
                    forceClearAbility(player, false, true);
                }
                continue;
            }

            if (JasonPsychoHandler.isJasonModeActive(player)) {
                forceExitForJasonMode(player);
                continue;
            }

            component.tickCooldown();
            if (!component.isActiveLike()) {
                continue;
            }

            component.tickPhase();
            clearAbilityFire(player);
            if (player.isUsingItem()) {
                /*
                 * 玩家可能在按 G 前一瞬间已经处于右键蓄力状态。
                 * 无恶不在期间不能左右键，所以服务端每 tick 兜底清掉 active item，
                 * 防止投掷物或其它长按物品在幽魂状态下继续蓄力/释放。
                 */
                player.clearActiveItem();
            }
            if (component.isEntering() && component.getPhaseTicks() >= JasonConstants.ABILITY_ENTER_TICKS) {
                component.markActive();
                broadcastSound(player.getServer(), JasonAbilitySoundS2CPacket.Action.START_LOOP);
                continue;
            }

            if (component.isFullyActive()
                    && component.getActiveTicks() >= JasonConstants.ABILITY_ENTER_TICKS
                    && player.age % JasonConstants.ABILITY_LOOP_SOUND_INTERVAL_TICKS == 0) {
                /*
                 * 周期性刷新 START_LOOP 是为了处理玩家中途进服或客户端声音被资源重载打断的情况。
                 * 客户端控制器会忽略已经在播放的循环声，不会每秒重启淡入。
                 */
                broadcastSound(player.getServer(), JasonAbilitySoundS2CPacket.Action.START_LOOP);
            }

            if (component.isExiting() && component.getPhaseTicks() >= JasonConstants.ABILITY_EXIT_TICKS) {
                component.finishExit(true);
                recordExitFinished(player);
                triggerScare(player);
            }
        }
    }

    /** 根据观看者分类刷新杰森失明；无恶不在结束后只停止续杯，让最后一份效果自然过期。 */
    private static void updateBlindnessEffect(
            @NotNull ServerPlayerEntity player,
            boolean hasActiveJason,
            boolean hasExitingJason
    ) {
        JasonAbilityBlindnessComponent effect = JasonAbilityBlindnessComponent.KEY.get(player);
        boolean enabled = hasActiveJason && (JasonAbilityRules.isAbilityActiveLike(player)
                ? JasonConstants.ABILITY_BLINDNESS_FOR_JASON_SELF
                : GameFunctions.isPlayerAliveAndSurvival(player)
                ? JasonConstants.ABILITY_BLINDNESS_FOR_OTHER_SURVIVORS
                : JasonConstants.ABILITY_BLINDNESS_FOR_NON_SURVIVAL);
        if (enabled) {
            /*
             * EXITING 阶段已经在 requestExit() 写入精确的 40 tick（2 秒）效果，
             * 这里必须保持它倒计时，不能再次续杯，也不能落入下面的清理分支。
             */
            if (!hasExitingJason) {
                effect.refreshOwnedEffect();
            }
        } else if (hasActiveJason) {
            effect.clearOwnedEffect();
        } else if (effect.isOwnedAndActive()) {
            effect.releaseOwnedEffectToExpireNaturally();
        }
    }

    private static void clearWorldBlindness(@NotNull ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            JasonAbilityBlindnessComponent.KEY.get(player).clearOwnedEffect();
        }
    }

    private static void tickScare(@NotNull ServerPlayerEntity player, @NotNull JasonAbilityPlayerComponent component) {
        int before = component.getScaredTicks();
        if (before <= 0) {
            return;
        }

        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            component.clearScared();
            return;
        }

        component.tickScared();
        if (component.getScaredTicks() <= 0) {
            recordScareEnded(player);
        }
    }

    private static void startAbility(@NotNull ServerPlayerEntity player, @NotNull JasonAbilityPlayerComponent component) {
        component.startEntering();
        clearAbilityFire(player);
        if (player.isUsingItem()) {
            player.clearActiveItem();
        }
        recordStarted(player);
        broadcastSound(player.getServer(), JasonAbilitySoundS2CPacket.Action.PLAY_START);
    }

    private static void requestExit(@NotNull ServerPlayerEntity player, @NotNull JasonAbilityPlayerComponent component) {
        component.startExiting();
        /*
         * 失明从按下主动解除的这一刻开始计时，持续时间与 2 秒 EXITING 过渡完全一致。
         * 这样不会在过渡结束后额外保留一段看不出显形变化的药水时间。
         */
        for (ServerPlayerEntity viewer : player.getServerWorld().getPlayers()) {
            JasonAbilityBlindnessComponent.KEY.get(viewer).startNaturalExitCountdown(JasonConstants.ABILITY_EXIT_TICKS);
        }
        recordExitRequested(player);
        broadcastSound(player.getServer(), JasonAbilitySoundS2CPacket.Action.STOP_LOOP);
        sendEndSound(player);
    }

    private static void forceClearAbility(@NotNull ServerPlayerEntity player, boolean startCooldown, boolean stopLoop) {
        boolean wasActive = JasonAbilityPlayerComponent.KEY.get(player).isActiveLike();
        if (stopLoop && wasActive) {
            broadcastSound(player.getServer(), JasonAbilitySoundS2CPacket.Action.STOP_LOOP);
        }
        JasonAbilityPlayerComponent.KEY.get(player).forceClear(startCooldown);
        if (wasActive) {
            clearWorldBlindness(player.getServerWorld());
        }
    }

    private static void triggerScare(@NotNull ServerPlayerEntity jason) {
        double radiusSquared = JasonConstants.ABILITY_SCARE_RADIUS_BLOCKS * JasonConstants.ABILITY_SCARE_RADIUS_BLOCKS;
        for (ServerPlayerEntity target : jason.getServerWorld().getPlayers()) {
            if (target.getUuid().equals(jason.getUuid())
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.squaredDistanceTo(jason) > radiusSquared) {
                continue;
            }

            JasonAbilityPlayerComponent.KEY.get(target).startScared(jason.getUuid());
            target.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DARKNESS,
                    JasonConstants.ABILITY_SCARE_TICKS,
                    JasonConstants.ABILITY_SCARE_DARKNESS_AMPLIFIER,
                    true,
                    true,
                    true
            ));
            recordScared(target, jason);
            sendSound(target, JasonAbilitySoundS2CPacket.Action.PLAY_JUMP_SCARE);
            sendSound(jason, JasonAbilitySoundS2CPacket.Action.PLAY_JUMP_SCARE);
        }
    }

    private static void sendEndSound(@NotNull ServerPlayerEntity jason) {
        for (ServerPlayerEntity recipient : jason.getServer().getPlayerManager().getPlayerList()) {
            if (recipient.getUuid().equals(jason.getUuid()) || !GameFunctions.isPlayerAliveAndSurvival(recipient)) {
                sendSound(recipient, JasonAbilitySoundS2CPacket.Action.PLAY_END);
            }
        }
    }

    private static void broadcastSound(@NotNull MinecraftServer server, @NotNull JasonAbilitySoundS2CPacket.Action action) {
        for (ServerPlayerEntity recipient : server.getPlayerManager().getPlayerList()) {
            sendSound(recipient, action);
        }
    }

    private static void sendSound(@NotNull ServerPlayerEntity recipient, @NotNull JasonAbilitySoundS2CPacket.Action action) {
        ServerPlayNetworking.send(recipient, new JasonAbilitySoundS2CPacket(action));
    }

    private static void recordStarted(@NotNull ServerPlayerEntity player) {
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.JASON_ABILITY_STARTED_EVENT, player, new NbtCompound());
    }

    private static void recordExitRequested(@NotNull ServerPlayerEntity player) {
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.JASON_ABILITY_EXIT_REQUESTED_EVENT, player, new NbtCompound());
    }

    private static void recordExitFinished(@NotNull ServerPlayerEntity player) {
        GameRecordManager.recordGlobalEvent(player.getServerWorld(), NoellesEventIds.JASON_ABILITY_EXIT_FINISHED_EVENT, player, new NbtCompound());
    }

    private static void recordScared(@NotNull ServerPlayerEntity victim, @NotNull ServerPlayerEntity jason) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("victim", victim.getUuid());
        GameRecordManager.recordGlobalEvent(victim.getServerWorld(), NoellesEventIds.JASON_ABILITY_SCARED_EVENT, jason, extra);
    }

    private static void recordScareEnded(@NotNull ServerPlayerEntity victim) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("victim", victim.getUuid());
        GameRecordManager.recordGlobalEvent(victim.getServerWorld(), NoellesEventIds.JASON_ABILITY_SCARE_ENDED_EVENT, victim, extra);
    }
}
