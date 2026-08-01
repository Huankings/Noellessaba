package org.agmas.noellesroles.roles.waiter;

import dev.doctor4t.wathe.block.DrinkTrayBlock;
import dev.doctor4t.wathe.api.task.MoodTaskApi;
import dev.doctor4t.wathe.index.WatheBlocks;
import dev.doctor4t.wathe.item.CocktailItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * 服务员可服务物品和 Wathe 心情任务之间的映射表。
 *
 * <p>交互、crosshair、回放和失败提示都通过这里识别“手上这个物品属于哪种服务”。
 * 这样新增物品时只需要补一个 ServiceType 和 getServiceType 的识别规则，不必在多个回调里重复判断。</p>
 */
public final class WaiterServiceItems {
    private WaiterServiceItems() {
    }

    public enum ServiceType {
        // consumeType 会传给 Wathe 的托盘效果接口，保证试剂/毒药知道这次物品是以什么方式被消耗的。
        COCKTAIL(MoodTaskApi.DRINK, "task.drink", "message.noellesroles.waiter.fail.drink", "drink_cocktail", false, false),
        FOOD(MoodTaskApi.EAT, "task.eat", "message.noellesroles.waiter.fail.food", "eat_food", false, false),
        POTION(MoodTaskApi.POTION, "task.potion", "message.noellesroles.waiter.fail.potion", "drink_potion", false, false),
        BAR_STOOL(MoodTaskApi.SIT, "task.sit", "message.noellesroles.waiter.fail.bar_stool", "waiter_bar_stool", true, false),
        FISHING_ROD(MoodTaskApi.FISH, "task.fish", "message.noellesroles.waiter.fail.fishing_rod", "waiter_fishing_rod", false, false),
        MUSIC_DISC(MoodTaskApi.MUSIC, "task.music", "message.noellesroles.waiter.fail.music_disc", "waiter_music_disc", true, false),
        CAMPFIRE(MoodTaskApi.FIRE, "task.fire", "message.noellesroles.waiter.fail.campfire", "waiter_campfire", true, false),
        SMOKER(MoodTaskApi.COOK, "task.cook", "message.noellesroles.waiter.fail.smoker", "waiter_smoker", true, false),
        SLEEPING_BAG(MoodTaskApi.SLEEP, "task.sleep", "message.noellesroles.waiter.fail.sleeping_bag", "waiter_sleeping_bag", true, true),
        BOOK(MoodTaskApi.BOOK, "task.book", "message.noellesroles.waiter.fail.book", "waiter_book", true, true);

        private final Identifier task;
        private final String taskTranslationKey;
        private final String failureTranslationKey;
        private final String consumeType;
        private final boolean selfUseWithoutTarget;
        private final boolean selfFallbackWhenTargetFails;

        /**
         * @param task 对应 Wathe 注册式心情任务 id。
         * @param taskTranslationKey 回放里显示的任务 lang key，例如 task.drink。
         * @param failureTranslationKey 目标或自己没有对应任务时的 actionbar lang key。
         * @param consumeType 递予成功后传入 TrayEffectHandler#onConsume 的消费类型标识。
         * @param selfUseWithoutTarget 是否允许不瞄准玩家时对自己使用。
         * @param selfFallbackWhenTargetFails 是否允许“瞄准别人但对方没有任务”时回退为自用。
         */
        ServiceType(
                Identifier task,
                String taskTranslationKey,
                String failureTranslationKey,
                String consumeType,
                boolean selfUseWithoutTarget,
                boolean selfFallbackWhenTargetFails
        ) {
            this.task = task;
            this.taskTranslationKey = taskTranslationKey;
            this.failureTranslationKey = failureTranslationKey;
            this.consumeType = consumeType;
            this.selfUseWithoutTarget = selfUseWithoutTarget;
            this.selfFallbackWhenTargetFails = selfFallbackWhenTargetFails;
        }

        public Identifier task() {
            return this.task;
        }

        public String taskTranslationKey() {
            return this.taskTranslationKey;
        }

        public String failureTranslationKey() {
            return this.failureTranslationKey;
        }

        public String consumeType() {
            return this.consumeType;
        }

        public boolean canSelfUseWithoutTarget() {
            return this.selfUseWithoutTarget;
        }

        public boolean canSelfFallbackWhenTargetFails() {
            return this.selfFallbackWhenTargetFails;
        }
    }

    public static boolean isServiceStack(ItemStack stack) {
        return getServiceType(stack) != null;
    }

    /**
     * 根据实际 ItemStack 识别服务类型。
     *
     * <p>鸡尾酒先于 FOOD 判断，因为 Wathe 的 CocktailItem 也可能带可食用/可饮用组件；
     * 先认鸡尾酒可以确保它映射到“去喝点东西”，而不是被普通食物分支抢走。</p>
     */
    public static @Nullable ServiceType getServiceType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof CocktailItem) {
            return ServiceType.COCKTAIL;
        }
        if (stack.get(DataComponentTypes.FOOD) != null && !(stack.getItem() instanceof CocktailItem)) {
            return ServiceType.FOOD;
        }
        if (stack.isOf(Items.POTION)) {
            return ServiceType.POTION;
        }
        if (stack.isOf(WatheBlocks.BAR_STOOL.asItem())) {
            return ServiceType.BAR_STOOL;
        }
        if (stack.isOf(Items.FISHING_ROD)) {
            return ServiceType.FISHING_ROD;
        }
        if (stack.isOf(Items.MUSIC_DISC_CREATOR)) {
            return ServiceType.MUSIC_DISC;
        }
        if (stack.isOf(Items.CAMPFIRE)) {
            return ServiceType.CAMPFIRE;
        }
        if (stack.isOf(Items.SMOKER)) {
            return ServiceType.SMOKER;
        }
        if (stack.isOf(ModItems.SLEEPING_BAG)) {
            return ServiceType.SLEEPING_BAG;
        }
        if (stack.isOf(ModItems.BOOK)) {
            return ServiceType.BOOK;
        }
        return null;
    }
}
