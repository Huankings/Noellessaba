package org.agmas.noellesroles.roles.cook;

import dev.doctor4t.wathe.api.task.MoodTaskApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.GameRecordTypes;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.waiter.WaiterDeliveryEffects;
import org.agmas.noellesroles.roles.waiter.WaiterServiceItems;
import org.jetbrains.annotations.Nullable;

/**
 * 厨师投喂食物的服务端交互入口。
 *
 * <p>服务员递予物品必须要求目标有对应需求；厨师的新机制正好相反：
 * 只要手里是食物，就可以直接投喂给准心玩家。如果目标恰好有“吃东西”任务，
 * 再额外帮目标完成任务并给厨师协助奖励。</p>
 */
public final class CookFeedingHandler {
    private static boolean initialized = false;

    private CookFeedingHandler() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        /*
         * 直接右键玩家时优先触发投喂。
         * 返回 CONSUME 可以阻止食物自己的右键食用逻辑继续执行，避免厨师想喂人却先把食物吃掉。
         */
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()
                    || !(player instanceof ServerPlayerEntity cook)
                    || !(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }

            ItemStack stack = cook.getStackInHand(hand);
            if (!isCookFeedStack(stack) || !canUseCookFeeding(cook)) {
                return ActionResult.PASS;
            }

            return tryFeedTarget(cook, target, hand, stack) ? ActionResult.CONSUME : ActionResult.PASS;
        });

        /*
         * 右键方块时也先尝试准心投喂。
         * 这能覆盖“目标身后刚好是方块”时，原版先处理方块交互而漏掉玩家的情况。
         */
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity cook)) {
                return ActionResult.PASS;
            }

            return tryHandleUse(cook, hand, cook.getStackInHand(hand)) ? ActionResult.CONSUME : ActionResult.PASS;
        });

        /*
         * 空挥右键时用射线再找一次目标。
         * 没有目标就放行，让厨师仍然可以正常自己吃食物。
         */
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity cook)) {
                return TypedActionResult.pass(player.getStackInHand(hand));
            }

            ItemStack stack = cook.getStackInHand(hand);
            if (tryHandleUse(cook, hand, stack)) {
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        });
    }

    private static boolean tryHandleUse(ServerPlayerEntity cook, Hand hand, ItemStack stack) {
        if (!isCookFeedStack(stack) || !canUseCookFeeding(cook)) {
            return false;
        }

        ServerPlayerEntity target = getTargetedPlayer(cook);
        return target != null && tryFeedTarget(cook, target, hand, stack);
    }

    private static boolean tryFeedTarget(ServerPlayerEntity cook, ServerPlayerEntity target, Hand hand, ItemStack stack) {
        if (target.equals(cook) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
            return true;
        }

        /*
         * 所有后续效果、actionbar 和回放都读取“成功投喂的这一份”快照。
         * 这样即使食物带自定义名称、毒药或托盘试剂，消耗手上物品后也不会丢数据。
         */
        ItemStack replaySnapshot = stack.copy();
        boolean completedEatTask = completeEatTaskIfPresent(target);

        WaiterDeliveryEffects.applyDeliveredStackEffects(
                target,
                replaySnapshot,
                WaiterServiceItems.ServiceType.FOOD
        );
        /*
         * 厨师投喂本身也算“目标吃了东西”，所以无论有没有完成任务，都要刷新厨师被动透视标记。
         */
        CookPlayerComponent.KEY.get(target).markAteFood();

        if (completedEatTask) {
            PlayerShopComponent.KEY.get(cook).addToBalance(CookConstants.FEED_HELP_BONUS);
        }

        decrementFedStack(cook, hand);
        sendFeedMessages(cook, target, replaySnapshot.getName(), completedEatTask);
        recordFeed(cook, target, replaySnapshot, completedEatTask);
        return true;
    }

    private static boolean isCookFeedStack(ItemStack stack) {
        return WaiterServiceItems.getServiceType(stack) == WaiterServiceItems.ServiceType.FOOD;
    }

    private static boolean canUseCookFeeding(ServerPlayerEntity player) {
        // 厨师投喂是职业主动交互，只允许对局内存活厨师使用，避免非厨师拿食物时抢走原版右键。
        return GameFunctions.isPlayerAliveAndSurvival(player)
                && GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.COOK);
    }

    private static @Nullable ServerPlayerEntity getTargetedPlayer(ServerPlayerEntity cook) {
        HitResult hitResult = ProjectileUtil.getCollision(
                cook,
                entity -> entity instanceof ServerPlayerEntity player
                        && !player.equals(cook)
                        && GameFunctions.isPlayerAliveAndSurvival(player)
                        /*
                         * 投喂属于玩家交互，不是伤害；尸体伪装等“不可交互目标”必须在服务端也被尊重。
                         */
                        && TargetVisibilityApi.canInteractWithPlayer(cook, player),
                CookConstants.FEED_TARGET_RANGE
        );
        if (hitResult instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() instanceof ServerPlayerEntity target) {
            return target;
        }
        return null;
    }

    private static boolean completeEatTaskIfPresent(ServerPlayerEntity target) {
        if (!MoodTaskApi.hasTask(target, MoodTaskApi.EAT)) {
            return false;
        }

        /*
         * 这里不抑制目标的任务收入：
         * 用户要求被投喂玩家按“正常完成任务”拿金币，厨师的 75 金币是额外协助奖励。
         */
        return MoodTaskApi.completeTask(target, MoodTaskApi.EAT, true).success();
    }

    private static void decrementFedStack(ServerPlayerEntity cook, Hand hand) {
        // 创造模式调试不消耗食物，正常对局每次成功投喂消耗 1 份。
        if (!cook.getAbilities().creativeMode) {
            cook.getStackInHand(hand).decrement(1);
        }
    }

    private static void sendFeedMessages(ServerPlayerEntity cook, ServerPlayerEntity target, Text itemName, boolean completedEatTask) {
        cook.sendMessage(
                Text.translatable("message.noellesroles.cook.feed", itemName, target.getDisplayName())
                        .withColor(CookConstants.ROLE_COLOR),
                true
        );
        target.sendMessage(
                Text.translatable(
                                completedEatTask
                                        ? "message.noellesroles.cook.fed_task"
                                        : "message.noellesroles.cook.fed",
                                cook.getDisplayName(),
                                itemName
                        )
                        .withColor(CookConstants.ROLE_COLOR),
                true
        );
    }

    private static void recordFeed(
            ServerPlayerEntity cook,
            ServerPlayerEntity target,
            ItemStack replaySnapshot,
            boolean completedEatTask
    ) {
        GameRecordManager.EventBuilder event = GameRecordManager.event(GameRecordTypes.GLOBAL_EVENT)
                .world(cook.getServerWorld())
                .actor(cook)
                .target(target)
                .put("event", NoellesEventIds.COOK_FEED_EVENT.toString())
                .put("item", Registries.ITEM.getId(replaySnapshot.getItem()).toString())
                .put("item_name", Text.Serialization.toJsonString(replaySnapshot.getName(), cook.getRegistryManager()))
                .putBool("completed_task", completedEatTask);

        if (completedEatTask) {
            event.put("task", "task.eat");
        }

        WaiterDeliveryEffects.EffectReplayInfo effectInfo = WaiterDeliveryEffects.getEffectReplayInfo(replaySnapshot);
        if (effectInfo != null) {
            event.put("effect_translation_key", effectInfo.translationKey());
            event.put("effect_fallback", effectInfo.fallback());
        }

        event.record();
    }
}
