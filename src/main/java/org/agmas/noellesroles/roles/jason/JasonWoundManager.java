package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 杰森重伤倒地 / 救治 / 处决的服务端权威逻辑。
 *
 * <p>投掷实体只负责发现“这次穿过了谁”，真正要不要倒地、奖励、救治倒计时、
 * 双重人格同步、匕首处决冷却豁免都统一收口到这里，方便后续平衡与排查。</p>
 */
public final class JasonWoundManager {
    private static final Set<UUID> KNIFE_EXECUTION_NO_COOLDOWN_PLAYERS = new HashSet<>();
    private static boolean initialized;

    private JasonWoundManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                tickWorld(world);
            }
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (isWoundedActionLocked(player)) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> isWoundedActionLocked(player) ? ActionResult.FAIL : ActionResult.PASS);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> isWoundedActionLocked(player) ? ActionResult.FAIL : ActionResult.PASS);
    }

    public static boolean isWoundedActionLocked(@Nullable PlayerEntity player) {
        /*
         * 用户明确要求：物品冷却、隐藏/限制之类只对 Wathe 定义的局内存活玩家生效。
         * 因此这里先走 isPlayerAliveAndSurvival，非存活旁观或创造玩家即使组件残留也不会被锁。
         */
        return player != null
                && GameFunctions.isPlayerAliveAndSurvival(player)
                && JasonWoundedPlayerComponent.KEY.get(player).isWounded();
    }

    public static boolean shouldBlockRevolverPickup(@Nullable PlayerEntity player, @Nullable ItemEntity itemEntity) {
        /*
         * 左轮掉落后的拾取发生在 ItemEntity 碰撞入口，不会经过使用物品回调。
         * 只对杰森重伤倒地玩家拦截 Wathe 左轮，避免误伤其它物品拾取和其它玩家捡枪。
         */
        return player != null
                && itemEntity != null
                && itemEntity.getStack().isOf(WatheItems.REVOLVER)
                && isWoundedActionLocked(player);
    }

    public static void handleThrowingWeaponHit(@NotNull ServerPlayerEntity thrower, @NotNull ServerPlayerEntity target, @NotNull ItemStack weaponStack) {
        if (!isAliveJason(thrower) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return;
        }
        if (thrower.getUuid().equals(target.getUuid())) {
            return;
        }

        JasonWoundedPlayerComponent targetComponent = JasonWoundedPlayerComponent.KEY.get(target);
        ItemStack replayStack = weaponStack.isEmpty() ? ModItems.THROWING_BLOOD_AXE.getDefaultStack() : weaponStack.copy();

        if (targetComponent.isWounded()) {
            /*
             * 倒地期间再次被杰森投掷武器贯穿：不再刷新倒地，直接按投掷武器穿杀处理。
             * 无论护盾/双重人格是否改写本次死亡，倒地状态都会解除，避免玩家卡在濒死姿势。
             */
            GameFunctions.killPlayer(
                    target,
                    true,
                    thrower,
                    NoellesDeathReasons.JASON_THROWING_WEAPON_DEATH_REASON,
                    GameFunctions.createReplayItemData(target.getServerWorld(), replayStack)
            );
            clearWoundWithDualMirror(target);
            return;
        }

        if (targetComponent.getWoundCount() >= 2) {
            GameFunctions.killPlayer(
                    target,
                    true,
                    thrower,
                    NoellesDeathReasons.JASON_THROWING_WEAPON_DEATH_REASON,
                    GameFunctions.createReplayItemData(target.getServerWorld(), replayStack)
            );
            if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
                addCoins(thrower, JasonConstants.DIRECT_KILL_EXTRA_REWARD_COINS);
            }
            return;
        }

        int nextWoundCount = targetComponent.getWoundCount() + 1;
        String weaponItemId = Registries.ITEM.getId(replayStack.getItem()).toString();
        targetComponent.markWounded(nextWoundCount, JasonConstants.BLEED_OUT_TICKS, thrower.getUuid(), weaponItemId);
        syncDualWound(target);
        dropRevolvers(target);
        addCoins(thrower, JasonConstants.WOUND_REWARD_COINS);
        recordWounded(target, thrower, replayStack);
    }

    public static void handleDeathAttempt(@NotNull dev.doctor4t.wathe.api.death.DeathContext context) {
        if (!(context.victim() instanceof ServerPlayerEntity victim)) {
            return;
        }

        JasonWoundedPlayerComponent component = JasonWoundedPlayerComponent.KEY.get(victim);
        boolean wasWounded = component.isWounded();
        boolean wasGasoline = component.isGasoline();

        if (wasWounded
                && context.confirmedDeath()
                && dev.doctor4t.wathe.game.GameConstants.DeathReasons.KNIFE.equals(context.deathReason())
                && context.serverKiller() != null
                && isAliveJason(context.serverKiller())) {
            /*
             * Wathe 的匕首 payload 会在 killPlayer 之后才写冷却。
             * 这里先记录“这次是杰森处决倒地玩家”，随后专门的 mixin 在 TAIL 消费该标记并清掉冷却。
             */
            KNIFE_EXECUTION_NO_COOLDOWN_PLAYERS.add(context.serverKiller().getUuid());
        }

        if (wasWounded && (context.confirmedDeath()
                || context.fatalIntercepted()
                || context.blockedByShield()
                || !NoellesDeathReasons.JASON_BLEEDING_TOO_MUCH_DEATH_REASON.equals(context.deathReason()))) {
            clearWoundWithDualMirror(victim);
        }

        if (wasGasoline && (context.confirmedDeath() || NoellesDeathReasons.JASON_BURN_DEATH_REASON.equals(context.deathReason()))) {
            component.clearGasoline();
        }
    }

    public static boolean consumeKnifeExecutionNoCooldown(@NotNull ServerPlayerEntity player) {
        return KNIFE_EXECUTION_NO_COOLDOWN_PLAYERS.remove(player.getUuid());
    }

    public static void clearWoundWithDualMirror(@NotNull ServerPlayerEntity player) {
        JasonWoundedPlayerComponent.KEY.get(player).clearWound();

        ServerPlayerEntity partner = getDualPartner(player);
        if (partner != null) {
            JasonWoundedPlayerComponent.KEY.get(partner).clearWound();
        }
        setDualWoundPause(player, false);
    }

    public static void resetPlayer(@NotNull PlayerEntity player) {
        JasonWoundedPlayerComponent.KEY.get(player).reset();
        /*
         * 匕首处决免冷却标记理论上会在同一个 payload 尾部消费。
         * 这里仍按玩家重置清一次，防止异常中断、断线或调试重置后把“下一次匕首不冷却”
         * 错误带到下一局。
         */
        KNIFE_EXECUTION_NO_COOLDOWN_PLAYERS.remove(player.getUuid());
        if (player instanceof ServerPlayerEntity serverPlayer) {
            /*
             * 双重人格轮换暂停字段存在世界组件里，不在 Jason 玩家组件内部。
             * 玩家重置时同步清掉这枚独立暂停标记，避免上一局倒地导致下一局人格切换倒计时仍被暂停。
             */
            setDualWoundPause(serverPlayer, false);
            /*
             * 一次性打火机只应该由本局已经落地的投掷油桶临时授予。
             * ResetPlayerEvent 通常也会重置背包，这里额外移除一次，避免调试重置或异常流程留下可用打火机。
             */
            JasonFireWorldComponent.removeOnceLighters(serverPlayer);
        }
    }

    public static void resetRoundTransientState() {
        /*
         * 回合结束时清空 Jason 的静态临时集合。
         * 这类数据不属于 CCA 组件，也不会被 Harpy 的玩家 reset 自动覆盖；
         * 放在回合 finalize 再清一次，可以兜住已经离线或未触发 ResetPlayerEvent 的玩家。
         */
        KNIFE_EXECUTION_NO_COOLDOWN_PLAYERS.clear();
    }

    private static void tickWorld(@NotNull ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            JasonWoundedPlayerComponent component = JasonWoundedPlayerComponent.KEY.get(player);
            if (!component.isWounded()) {
                continue;
            }

            if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
                clearWoundWithDualMirror(player);
                continue;
            }

            maintainWoundedPose(player);
            if (isNonActiveDualPersonality(player)) {
                continue;
            }

            tickBleedAndRescue(player, component);
        }
    }

    private static void maintainWoundedPose(@NotNull ServerPlayerEntity player) {
        /*
         * 服务端每 tick 兜底清掉会改变局势的动作状态。
         * 背包、丢弃、换槽等整理行为不在这里处理，保留用户要求的“背包整理能力”。
         */
        player.setSprinting(false);
        if (player.isUsingItem()) {
            player.clearActiveItem();
        }
        if (player.getPose() != EntityPose.SWIMMING) {
            player.setPose(EntityPose.SWIMMING);
        }
    }

    private static void tickBleedAndRescue(@NotNull ServerPlayerEntity wounded, @NotNull JasonWoundedPlayerComponent component) {
        ServerPlayerEntity rescuer = findRescuer(wounded, component.getRescuerUuid());
        if (rescuer != null) {
            int nextProgress = component.getRescueTicks() + 1;
            component.setRescueProgress(rescuer.getUuid(), nextProgress);
            syncDualWound(wounded);
            if (nextProgress >= getRequiredRescueTicks(component.getWoundCount(), rescuer)) {
                rescue(wounded, rescuer);
            }
            return;
        }

        component.clearRescueProgress();
        int bleedTicks = component.getBleedTicks() - 1;
        component.setBleedTicks(bleedTicks);
        syncDualWound(wounded);
        if (bleedTicks <= 0) {
            bleedOut(wounded, component);
        }
    }

    private static void rescue(@NotNull ServerPlayerEntity wounded, @NotNull ServerPlayerEntity rescuer) {
        clearWoundWithDualMirror(wounded);
        applyRescuedEffects(wounded);
        ServerPlayerEntity partner = getDualPartner(wounded);
        if (partner != null && GameFunctions.isPlayerAliveAndSurvival(partner)) {
            applyRescuedEffects(partner);
        }
        recordRescued(wounded, rescuer);
    }

    private static void bleedOut(@NotNull ServerPlayerEntity wounded, @NotNull JasonWoundedPlayerComponent component) {
        ServerPlayerEntity attacker = component.getAttackerUuid() == null
                ? null
                : wounded.getServer().getPlayerManager().getPlayer(component.getAttackerUuid());
        GameFunctions.killPlayer(
                wounded,
                true,
                attacker,
                NoellesDeathReasons.JASON_BLEEDING_TOO_MUCH_DEATH_REASON,
                new NbtCompound()
        );
        clearWoundWithDualMirror(wounded);
    }

    private static void applyRescuedEffects(@NotNull ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.DARKNESS,
                JasonConstants.RESCUED_EFFECT_TICKS,
                0,
                true,
                true,
                true
        ));
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS,
                JasonConstants.RESCUED_EFFECT_TICKS,
                JasonConstants.RESCUED_SLOWNESS_AMPLIFIER,
                true,
                true,
                true
        ));
    }

    private static int getRequiredRescueTicks(int woundCount, @NotNull ServerPlayerEntity rescuer) {
        int base = woundCount >= 2 ? JasonConstants.SECOND_RESCUE_TICKS : JasonConstants.FIRST_RESCUE_TICKS;
        if (isHoldingMedicalKit(rescuer)) {
            return Math.max(1, (int) Math.round(base * JasonConstants.MEDICAL_KIT_RESCUE_TIME_MULTIPLIER));
        }
        return base;
    }

    private static boolean isHoldingMedicalKit(@NotNull ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.MEDICAL_KIT) || player.getOffHandStack().isOf(ModItems.MEDICAL_KIT);
    }

    private static @Nullable ServerPlayerEntity findRescuer(@NotNull ServerPlayerEntity wounded, @Nullable UUID currentRescuerUuid) {
        if (currentRescuerUuid != null) {
            ServerPlayerEntity current = wounded.getServer().getPlayerManager().getPlayer(currentRescuerUuid);
            if (current != null && isValidRescuer(current, wounded)) {
                return current;
            }
        }

        for (ServerPlayerEntity candidate : wounded.getServerWorld().getPlayers()) {
            if (isValidRescuer(candidate, wounded)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isValidRescuer(@NotNull ServerPlayerEntity rescuer, @NotNull ServerPlayerEntity wounded) {
        if (rescuer.getUuid().equals(wounded.getUuid())) {
            return false;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(rescuer) || !rescuer.isSneaking()) {
            return false;
        }
        if (rescuer.squaredDistanceTo(wounded) > JasonConstants.RESCUE_RANGE_BLOCKS * JasonConstants.RESCUE_RANGE_BLOCKS) {
            return false;
        }
        if (!TargetVisibilityApi.canTargetPlayer(rescuer, wounded)) {
            return false;
        }
        return isLookingAt(rescuer, wounded);
    }

    private static boolean isLookingAt(@NotNull ServerPlayerEntity viewer, @NotNull ServerPlayerEntity target) {
        Vec3d eye = viewer.getEyePos();
        Vec3d targetCenter = target.getBoundingBox().getCenter();
        Vec3d delta = targetCenter.subtract(eye);
        double length = delta.length();
        if (length <= 0.0001D || length > JasonConstants.RESCUE_RANGE_BLOCKS) {
            return false;
        }
        double dot = viewer.getRotationVec(1.0F).normalize().dotProduct(delta.normalize());
        return dot >= JasonConstants.RESCUE_LOOK_DOT_MIN;
    }

    private static void dropRevolvers(@NotNull ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        dropRevolversFromList(player, inventory.main);
        dropRevolversFromList(player, inventory.offHand);
        player.currentScreenHandler.sendContentUpdates();
    }

    private static void dropRevolversFromList(@NotNull ServerPlayerEntity player, java.util.List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isOf(WatheItems.REVOLVER)) {
                continue;
            }
            player.dropItem(stack.copy(), true, false);
            stacks.set(i, ItemStack.EMPTY);
        }
    }

    private static void syncDualWound(@NotNull ServerPlayerEntity player) {
        ServerPlayerEntity partner = getDualPartner(player);
        if (partner != null) {
            JasonWoundedPlayerComponent.KEY.get(partner).copyWoundFrom(JasonWoundedPlayerComponent.KEY.get(player));
        }
        setDualWoundPause(player, true);
    }

    private static @Nullable ServerPlayerEntity getDualPartner(@NotNull ServerPlayerEntity player) {
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.getWorld());
        UUID partnerUuid = component.getPartner(player.getUuid());
        return partnerUuid == null ? null : player.getServer().getPlayerManager().getPlayer(partnerUuid);
    }

    private static boolean isNonActiveDualPersonality(@NotNull ServerPlayerEntity player) {
        DualPersonalityComponent.PairState pair = DualPersonalityComponent.KEY.get(player.getWorld()).getPair(player.getUuid());
        return pair != null && !pair.doubleActive && pair.isDormant(player.getUuid());
    }

    private static void setDualWoundPause(@NotNull ServerPlayerEntity player, boolean paused) {
        DualPersonalityComponent component = DualPersonalityComponent.KEY.get(player.getWorld());
        DualPersonalityComponent.PairState pair = component.getPair(player.getUuid());
        if (pair == null || pair.doubleActive || pair.jasonWoundedPaused == paused) {
            return;
        }
        /*
         * 重伤倒地期间暂停双重人格普通轮换倒计时，但继续维持 active/dormant 相机关系。
         * 这里使用独立字段，不复用掉线 paused，避免重连逻辑误把倒地暂停当成离线暂停处理。
         */
        pair.jasonWoundedPaused = paused;
        component.sync();
    }

    private static void recordWounded(@NotNull ServerPlayerEntity victim, @NotNull ServerPlayerEntity thrower, @NotNull ItemStack weaponStack) {
        NbtCompound extra = GameFunctions.createReplayItemData(victim.getServerWorld(), weaponStack);
        extra.putUuid("victim", victim.getUuid());
        GameRecordManager.recordGlobalEvent(victim.getServerWorld(), NoellesEventIds.JASON_WOUNDED_EVENT, thrower, extra);
    }

    private static void recordRescued(@NotNull ServerPlayerEntity victim, @NotNull ServerPlayerEntity rescuer) {
        NbtCompound extra = new NbtCompound();
        extra.putUuid("victim", victim.getUuid());
        GameRecordManager.recordGlobalEvent(victim.getServerWorld(), NoellesEventIds.JASON_RESCUED_EVENT, rescuer, extra);
    }

    private static boolean isAliveJason(@NotNull ServerPlayerEntity player) {
        return GameFunctions.isPlayerAliveAndSurvival(player)
                && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.JASON);
    }

    private static void addCoins(@NotNull ServerPlayerEntity player, int coins) {
        if (coins <= 0 || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }
        PlayerShopComponent.KEY.get(player).addToBalance(coins);
    }

    public static int getRemainingBleedSeconds(@NotNull JasonWoundedPlayerComponent component) {
        return Math.max(0, (component.getBleedTicks() + 19) / 20);
    }

    public static int getRemainingRescueSeconds(@NotNull JasonWoundedPlayerComponent component, @Nullable PlayerEntity rescuer) {
        int base = component.getWoundCount() >= 2 ? JasonConstants.SECOND_RESCUE_TICKS : JasonConstants.FIRST_RESCUE_TICKS;
        /*
         * 救治耗时取决于救治者是否手持医疗箱。
         * 倒地本人 HUD 有时只能拿到 rescuerUuid 而不一定能解析出实体，因此允许 rescuer 为空；
         * 为空时使用基础救治时间作为保守兜底，避免 HUD 因客户端实体暂缺直接消失。
         */
        int required = (rescuer != null && (rescuer.getMainHandStack().isOf(ModItems.MEDICAL_KIT) || rescuer.getOffHandStack().isOf(ModItems.MEDICAL_KIT)))
                ? Math.max(1, (int) Math.round(base * JasonConstants.MEDICAL_KIT_RESCUE_TIME_MULTIPLIER))
                : base;
        return Math.max(0, (required - component.getRescueTicks() + 19) / 20);
    }

    public static boolean isRescuing(@NotNull JasonWoundedPlayerComponent component, @NotNull PlayerEntity viewer) {
        return component.getRescuerUuid() != null && component.getRescuerUuid().equals(viewer.getUuid()) && component.getRescueTicks() > 0;
    }

    public static boolean isBeingRescuedByOther(@NotNull JasonWoundedPlayerComponent component, @NotNull PlayerEntity viewer) {
        return component.getRescuerUuid() != null && !component.getRescuerUuid().equals(viewer.getUuid()) && component.getRescueTicks() > 0;
    }
}
