package org.agmas.noellesroles.roles.waiter;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.api.tray.TrayEffectHandler;
import dev.doctor4t.wathe.api.tray.TrayEffectRegistry;
import dev.doctor4t.wathe.api.task.MoodTaskApi;
import dev.doctor4t.wathe.api.task.TaskCompletionApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import dev.doctor4t.wathe.util.TrayEffectUtils;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.projectile.ProjectileUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 服务员服务交互的服务端总入口。
 *
 * <p>这个类负责三类事情：</p>
 * <p>1. 拦截服务员手持服务物品的右键，优先按“递给目标玩家”处理；没有目标时再按允许自用的物品处理。</p>
 * <p>2. 成功完成 Wathe 心情任务、发金币、回血情绪、消耗物品，并把托盘毒药/试剂效果带给目标。</p>
 * <p>3. 写入回放事件和服务员被动透视状态，让回放与客户端本能显示都能读到这次服务。</p>
 */
public final class WaiterInteractionHandler {
    /*
     * 服务员“帮别人完成任务”时，目标玩家不能因为这次交互额外拿任务收入金币。
     * Wathe 的任务完成入口会统一发钱，所以这里用 ThreadLocal 临时标记“这一帧这个玩家的这个任务要跳过收入”。
     * 旧版靠 WaiterTaskIncomeMixin 拦截 TaskCompletionApi；现在改为注册 Wathe 公开的收入规则，
     * 不再依赖 TaskCompletionApi 的私有执行顺序或 PlayerMoodComponent 私有方法 invoker。
     */
    private static final ThreadLocal<Set<SuppressedTaskIncome>> SUPPRESSED_TASK_INCOME =
            ThreadLocal.withInitial(HashSet::new);
    private static boolean initialized = false;

    private WaiterInteractionHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // 任意玩家完成心情任务都会短暂暴露给服务员；客户端再判断观看者是不是服务员。
        TaskCompletionApi.AFTER_TASK_COMPLETE.register(context ->
                WaiterPlayerComponent.KEY.get(context.player()).revealToWaiters()
        );

        /*
         * 服务员帮别人完成任务时，目标仍会正常触发 AFTER_TASK_COMPLETE，
         * 但 Wathe 默认任务收入会被这里跳过；服务员自己的 25 金币在服务成功后单独发放。
         */
        TaskCompletionApi.registerTaskIncomeRule(
                NoellesRolesCore.id("waiter/suppress_served_task_income"),
                TaskCompletionApi.DEFAULT_PRIORITY + 100,
                context -> shouldSuppressServedTaskIncome(context.player(), context.taskId())
                        ? TaskCompletionApi.TaskIncomeDecision.SUPPRESS_DEFAULT_INCOME
                        : TaskCompletionApi.TaskIncomeDecision.PASS
        );

        /*
         * 直接右键实体时优先走这里。
         * 返回 CONSUME 可以阻止原物品本身的右键逻辑继续触发，满足“服务员手持上述物品对准玩家时先拦截原本用途”。
         */
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()
                    || !(player instanceof ServerPlayerEntity waiter)
                    || !(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);
            WaiterServiceItems.ServiceType serviceType = WaiterServiceItems.getServiceType(stack);
            if (serviceType == null || !canUseWaiterService(waiter)) {
                return ActionResult.PASS;
            }

            return tryServeTarget(waiter, target, hand, stack, serviceType)
                    ? ActionResult.CONSUME
                    : ActionResult.PASS;
        });

        /*
         * 右键方块时也尝试服务逻辑。
         * 这能覆盖吧凳、营火、烟熏炉等原本会放置方块的物品，避免服务员想服务时先把方块放出去。
         */
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            return tryHandleUse(serverPlayer, hand, player.getStackInHand(hand))
                    ? ActionResult.CONSUME
                    : ActionResult.PASS;
        });

        // 空挥右键时用于处理“没有瞄准玩家的自用”，例如睡袋、图书、唱片、营火等。
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            ItemStack stack = player.getStackInHand(hand);
            if (tryHandleUse(serverPlayer, hand, stack)) {
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        });
    }

    public static boolean shouldSuppressServedTaskIncome(ServerPlayerEntity player, Identifier task) {
        // 只消费一次标记，避免同一个玩家之后自然完成同类任务时也被错误免收入。
        Set<SuppressedTaskIncome> suppressed = SUPPRESSED_TASK_INCOME.get();
        boolean removed = suppressed.remove(new SuppressedTaskIncome(player.getUuid(), task));
        if (suppressed.isEmpty()) {
            SUPPRESSED_TASK_INCOME.remove();
        }
        return removed;
    }

    private static boolean tryHandleUse(ServerPlayerEntity waiter, Hand hand, ItemStack stack) {
        // 非服务物品或非服务员直接放行，让原版/Wathe/其他职业逻辑继续处理。
        WaiterServiceItems.ServiceType serviceType = WaiterServiceItems.getServiceType(stack);
        if (serviceType == null || !canUseWaiterService(waiter)) {
            return false;
        }

        // 服务员交互总是先找原版攻击距离内的玩家目标；有目标时按“递给别人”处理。
        ServerPlayerEntity target = getTargetedPlayer(waiter);
        if (target != null) {
            return tryServeTarget(waiter, target, hand, stack, serviceType);
        }

        // 没有目标时，只有需求指定可自用的物品才会拦截右键。
        if (!serviceType.canSelfUseWithoutTarget()) {
            return false;
        }
        return tryServeSelf(waiter, hand, stack, serviceType);
    }

    private static boolean tryServeTarget(
            ServerPlayerEntity waiter,
            ServerPlayerEntity target,
            Hand hand,
            ItemStack stack,
            WaiterServiceItems.ServiceType serviceType
    ) {
        if (!GameFunctions.isPlayerAliveAndSurvival(target)) {
            return true;
        }

        /*
         * 目标没有对应任务时显示对应“不渴/不饿/不困”等失败文案。
         * 睡袋和图书额外支持“瞄准别人但对方没有任务时，如果自己有任务则自用”。
         */
        if (!hasTask(target, serviceType.task())) {
            if (serviceType.canSelfFallbackWhenTargetFails() && hasTask(waiter, serviceType.task())) {
                return completeSelfUse(waiter, hand, stack, serviceType);
            }
            sendFailure(waiter, target, serviceType.failureTranslationKey());
            return true;
        }

        // 烟熏炉任务还要求目标背包里有能被熔炼或烟熏为食物的生食；成功前先消耗 1 个。
        if (serviceType == WaiterServiceItems.ServiceType.SMOKER && !consumeRawFood(target)) {
            sendFailure(waiter, target, "message.noellesroles.waiter.fail.smoker_no_raw");
            return true;
        }

        /*
         * 先复制快照再消耗物品。
         * 回放、试剂、毒药都必须读取“成功递予的那一份”自己的 NBT/组件，不能在 decrement 后再从手上取。
         */
        ItemStack replaySnapshot = stack.copy();
        if (!completeTask(target, serviceType.task(), true)) {
            sendFailure(waiter, target, serviceType.failureTranslationKey());
            return true;
        }
        WaiterDeliveryEffects.applyDeliveredStackEffects(target, replaySnapshot, serviceType);
        if (serviceType == WaiterServiceItems.ServiceType.SLEEPING_BAG) {
            blind(target);
        }

        // 帮别人完成任务时，目标收入被 suppress，服务员本人一次性拿 25。
        PlayerShopComponent.KEY.get(waiter).addToBalance(WaiterConstants.SERVE_OTHER_INCOME);
        decrementServedStack(waiter, hand);
        sendServeSuccess(waiter, target, replaySnapshot.getName());
        recordServe(waiter, target, replaySnapshot, serviceType);
        return true;
    }

    private static boolean tryServeSelf(
            ServerPlayerEntity waiter,
            Hand hand,
            ItemStack stack,
            WaiterServiceItems.ServiceType serviceType
    ) {
        if (!hasTask(waiter, serviceType.task())) {
            sendFailure(waiter, waiter, serviceType.failureTranslationKey());
            return true;
        }

        // 自用烟熏炉同样需要自己背包里有生食，并消耗掉这一份生食。
        if (serviceType == WaiterServiceItems.ServiceType.SMOKER && !consumeRawFood(waiter)) {
            sendFailure(waiter, waiter, "message.noellesroles.waiter.fail.smoker_no_raw");
            return true;
        }

        return completeSelfUse(waiter, hand, stack, serviceType);
    }

    private static boolean completeSelfUse(
            ServerPlayerEntity waiter,
            Hand hand,
            ItemStack stack,
            WaiterServiceItems.ServiceType serviceType
    ) {
        ItemStack replaySnapshot = stack.copy();
        // 自己满足自己的任务不抑制任务收入，仍按 Noelles 规则由 TaskCompletionApi 发 50 金币。
        if (!completeTask(waiter, serviceType.task(), false)) {
            sendFailure(waiter, waiter, serviceType.failureTranslationKey());
            return true;
        }
        if (serviceType == WaiterServiceItems.ServiceType.SLEEPING_BAG) {
            blind(waiter);
        }
        decrementServedStack(waiter, hand);
        waiter.sendMessage(Text.translatable("message.noellesroles.waiter.self").withColor(WaiterConstants.ROLE_COLOR), true);
        recordSelfUse(waiter, replaySnapshot, serviceType);
        return true;
    }

    private static boolean canUseWaiterService(ServerPlayerEntity player) {
        // 只允许对局内、存活、服务员身份的玩家触发这些特殊右键。
        return GameFunctions.isPlayerAliveAndSurvival(player)
                && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.WAITER);
    }

    private static @Nullable ServerPlayerEntity getTargetedPlayer(ServerPlayerEntity waiter) {
        // 使用 ProjectileUtil 模拟准星射线，范围固定为服务员需求中的原版玩家攻击距离。
        HitResult hitResult = ProjectileUtil.getCollision(
                waiter,
                entity -> entity instanceof ServerPlayerEntity player
                        && !player.equals(waiter)
                        && GameFunctions.isPlayerAliveAndSurvival(player)
                        /*
                         * 服务员的右键服务是玩家交互而不是攻击。
                         * 尸体伪装这类状态会在 TargetVisibilityApi 里声明“不要把我当作活人目标”，
                         * 所以这里也必须尊重 canInteractWithPlayer，避免准心不高亮但服务端仍然能递东西暴露身份。
                         */
                        && TargetVisibilityApi.canInteractWithPlayer(waiter, player),
                WaiterConstants.INTERACTION_RANGE
        );
        if (hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof ServerPlayerEntity target) {
            return target;
        }
        return null;
    }

    private static boolean hasTask(ServerPlayerEntity player, Identifier task) {
        return MoodTaskApi.hasTask(player, task);
    }

    private static boolean completeTask(ServerPlayerEntity player, Identifier task, boolean suppressIncome) {
        /*
         * PlayerMoodComponent#completeTask 已经由 Wathe 新开放的 MoodTaskApi 代理。
         * 这里直接调用公开 API，完整走 Wathe 的任务移除、心情回复、任务完成事件、回放和同步流程。
         * suppressIncome 只影响 TaskCompletionApi 的默认收入，不会取消 AFTER_TASK_COMPLETE。
         */
        if (suppressIncome) {
            SUPPRESSED_TASK_INCOME.get().add(new SuppressedTaskIncome(player.getUuid(), task));
        }

        boolean completed;
        try {
            completed = MoodTaskApi.completeTask(player, task, true).success();
        } finally {
            if (suppressIncome) {
                Set<SuppressedTaskIncome> suppressed = SUPPRESSED_TASK_INCOME.get();
                suppressed.remove(new SuppressedTaskIncome(player.getUuid(), task));
                if (suppressed.isEmpty()) {
                    SUPPRESSED_TASK_INCOME.remove();
                }
            }
        }

        if (completed) {
            // 服务员服务完成的任务也算“完成心情任务”，所以同样触发 4 秒可见。
            WaiterPlayerComponent.KEY.get(player).revealToWaiters();
        }
        return completed;
    }

    private static void applyDeliveredStackEffects(
            ServerPlayerEntity target,
            ItemStack replaySnapshot,
            WaiterServiceItems.ServiceType serviceType
    ) {
        // Wathe 原毒药不是普通 trayEffect，而是 POISONER 组件，所以先单独处理。
        applyPoisonEffect(target, replaySnapshot);
        Identifier trayEffectId = TrayEffectUtils.getTrayEffectId(replaySnapshot);
        if (trayEffectId == null) {
            return;
        }

        TrayEffectHandler handler = TrayEffectRegistry.getByEffectId(trayEffectId);
        if (handler != null) {
            // defense_vial/delusion_vial/sedative 等托盘效果都会通过 Wathe 的统一接口应用到目标。
            handler.onConsume(target, replaySnapshot, serviceType.consumeType(), TrayEffectUtils.getTrayEffectOwner(replaySnapshot));
        }
    }

    private static void applyPoisonEffect(ServerPlayerEntity target, ItemStack replaySnapshot) {
        // 没有 POISONER 组件代表这份物品没有毒，不做任何额外处理。
        String poisoner = replaySnapshot.getOrDefault(WatheDataComponentTypes.POISONER, null);
        if (poisoner == null) {
            return;
        }

        UUID poisonerUuid;
        try {
            poisonerUuid = UUID.fromString(poisoner);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        PlayerPoisonComponent poison = PlayerPoisonComponent.KEY.get(target);
        int currentPoisonTicks = poison.poisonTicks;
        /*
         * Wathe 毒药第一次中毒会随机生成结算时间；如果已经中毒，则服务员再次递毒会把结算提前一段随机时间。
         * 这样保留 Wathe 原毒药节奏，同时让多次递毒不会重置成更晚的时间。
         */
        int poisonTicks = currentPoisonTicks == -1
                ? target.getWorld().getRandom().nextBetween(PlayerPoisonComponent.clampTime.getLeft(), PlayerPoisonComponent.clampTime.getRight())
                : net.minecraft.util.math.MathHelper.clamp(
                        currentPoisonTicks - target.getWorld().getRandom().nextBetween(
                                WaiterConstants.POISON_STACK_ACCELERATION_MIN_TICKS,
                                WaiterConstants.POISON_STACK_ACCELERATION_MAX_TICKS
                        ),
                        0,
                        PlayerPoisonComponent.clampTime.getRight()
                );

        NbtCompound poisonData = new NbtCompound();
        poisonData.putString("item", Registries.ITEM.getId(replaySnapshot.getItem()).toString());
        poisonData.putString("item_name", Text.Serialization.toJsonString(replaySnapshot.getName(), target.getRegistryManager()));
        poison.setDetailedPoisonTicks(poisonTicks, poisonerUuid, GameConstants.DeathReasons.POISON, poisonData);
    }

    private static boolean consumeRawFood(ServerPlayerEntity target) {
        // 扫描目标整个背包，找到第一份“能烤熟成食物”的物品并消耗 1 个。
        for (int slot = 0; slot < target.getInventory().size(); slot++) {
            ItemStack stack = target.getInventory().getStack(slot);
            if (!isCookableFood(target.getServerWorld(), stack)) {
                continue;
            }
            stack.decrement(1);
            return true;
        }
        return false;
    }

    private static boolean isCookableFood(ServerWorld world, ItemStack stack) {
        // 同时检查熔炉和烟熏炉配方，避免硬编码原版生肉列表，也兼容数据包/模组添加的可烹饪食物。
        return hasFoodCookingRecipe(world, stack, RecipeType.SMELTING)
                || hasFoodCookingRecipe(world, stack, RecipeType.SMOKING);
    }

    private static boolean hasFoodCookingRecipe(
            ServerWorld world,
            ItemStack stack,
            RecipeType<? extends Recipe<SingleStackRecipeInput>> recipeType
    ) {
        SingleStackRecipeInput recipeInput = new SingleStackRecipeInput(stack);
        Optional<? extends RecipeEntry<? extends Recipe<SingleStackRecipeInput>>> recipe =
                world.getRecipeManager().getFirstMatch(recipeType, recipeInput, world);
        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack output = recipe.get().value().getResult(world.getRegistryManager());
        // 只接受烹饪结果带 FOOD 组件的配方，防止把矿石、仙人掌等非食物也当成“生食”消耗。
        return !output.isEmpty() && output.get(net.minecraft.component.DataComponentTypes.FOOD) != null;
    }

    private static void decrementServedStack(ServerPlayerEntity waiter, Hand hand) {
        // 创造模式调试不消耗物品，正常对局按一次成功服务消耗一份。
        if (!waiter.getAbilities().creativeMode) {
            waiter.getStackInHand(hand).decrement(1);
        }
    }

    private static void blind(ServerPlayerEntity target) {
        // 睡袋无论递给别人还是自用成功，都让被满足睡眠任务的玩家失明 4 秒。
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, WaiterConstants.BLINDNESS_TICKS, WaiterConstants.BASE_EFFECT_AMPLIFIER, false, true, true));
    }

    private static void sendServeSuccess(ServerPlayerEntity waiter, ServerPlayerEntity target, Text itemName) {
        waiter.sendMessage(
                Text.translatable("message.noellesroles.waiter.serve", itemName, target.getDisplayName(), target.getDisplayName())
                        .withColor(WaiterConstants.ROLE_COLOR),
                true
        );
        target.sendMessage(
                Text.translatable("message.noellesroles.waiter.served", waiter.getDisplayName(), itemName)
                        .withColor(WaiterConstants.ROLE_COLOR),
                true
        );
    }

    private static void sendFailure(ServerPlayerEntity waiter, ServerPlayerEntity target, String translationKey) {
        waiter.sendMessage(
                Text.translatable(translationKey, target.getDisplayName()).withColor(WaiterConstants.ROLE_COLOR),
                true
        );
    }

    private static void recordServe(
            ServerPlayerEntity waiter,
            ServerPlayerEntity target,
            ItemStack replaySnapshot,
            WaiterServiceItems.ServiceType serviceType
    ) {
        // 回放事件保存 item id、物品显示名和任务 lang key，格式化时再按客户端语言渲染文本。
        GameRecordManager.EventBuilder event = GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                .world(waiter.getServerWorld())
                .actor(waiter)
                .target(target)
                .put("event", NoellesEventIds.WAITER_SERVE_EVENT.toString())
                .put("item", Registries.ITEM.getId(replaySnapshot.getItem()).toString())
                .put("item_name", Text.Serialization.toJsonString(replaySnapshot.getName(), waiter.getRegistryManager()))
                .put("task", serviceType.taskTranslationKey());

        WaiterDeliveryEffects.EffectReplayInfo effectInfo = WaiterDeliveryEffects.getEffectReplayInfo(replaySnapshot);
        if (effectInfo != null) {
            event.put("effect_translation_key", effectInfo.translationKey());
            event.put("effect_fallback", effectInfo.fallback());
        }
        event.record();
    }

    private static void recordSelfUse(
            ServerPlayerEntity waiter,
            ItemStack replaySnapshot,
            WaiterServiceItems.ServiceType serviceType
    ) {
        // 自用只记录 actor，不记录 target；直接食用的鸡尾酒/食物/药水仍走原物品逻辑，不会进这个事件。
        GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                .world(waiter.getServerWorld())
                .actor(waiter)
                .put("event", NoellesEventIds.WAITER_SELF_USE_EVENT.toString())
                .put("item", Registries.ITEM.getId(replaySnapshot.getItem()).toString())
                .put("item_name", Text.Serialization.toJsonString(replaySnapshot.getName(), waiter.getRegistryManager()))
                .put("task", serviceType.taskTranslationKey())
                .record();
    }

    private static @Nullable EffectReplayInfo getEffectReplayInfo(ItemStack replaySnapshot) {
        // 回放里需要把“带有[某试剂/毒药]”显示出来，所以从物品自己的组件里反查效果名。
        if (replaySnapshot.contains(WatheDataComponentTypes.POISONER)) {
            return new EffectReplayInfo(WatheItems.POISON_VIAL.getTranslationKey(), "Poison");
        }

        Identifier trayEffectId = TrayEffectUtils.getTrayEffectId(replaySnapshot);
        if (trayEffectId == null) {
            return null;
        }
        if (trayEffectId.equals(NoellesEventIds.DEFENSE_TRAY_EFFECT)) {
            return new EffectReplayInfo("item.noellesroles.defense_vial", "Defense Vial");
        }
        if (trayEffectId.equals(NoellesEventIds.DELUSION_TRAY_EFFECT)) {
            return new EffectReplayInfo("item.noellesroles.delusion_vial", "Delusion Vial");
        }
        if (trayEffectId.equals(NoellesEventIds.SEDATIVE_TRAY_EFFECT)) {
            return new EffectReplayInfo("item.noellesroles.sedative", "Sedative");
        }
        if (trayEffectId.equals(NoellesEventIds.TIMED_BOMB_TRAY_EMBEDDED_EVENT)) {
            return new EffectReplayInfo("item.noellesroles.timed_bomb", "Timed Bomb");
        }
        return new EffectReplayInfo("effect." + trayEffectId.toString().replace(':', '.'), trayEffectId.getPath());
    }

    private record SuppressedTaskIncome(UUID playerUuid, Identifier task) {
    }

    private record EffectReplayInfo(String translationKey, String fallback) {
    }
}
