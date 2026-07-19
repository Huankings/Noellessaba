package org.agmas.noellesroles.roles.waiter;

import dev.doctor4t.wathe.index.WatheBlocks;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import org.agmas.noellesroles.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 服务员专属商店的条目和购买后实际交付物生成逻辑。
 *
 * <p>商店界面中的 random_food/random_drink/random_potion 是图标物品；
 * 玩家真正购买时会在 NoellesRolesShops 中调用这里的方法，替换成随机食物、随机鸡尾酒或随机药水。</p>
 */
public final class WaiterShopHandler {
    // 直接复用 Wathe 已注册的 CocktailItem，避免在 NoellesRoles 中复制鸡尾酒实现。
    private static final List<Item> RANDOM_COCKTAILS = List.of(
            WatheItems.OLD_FASHIONED,
            WatheItems.MOJITO,
            WatheItems.MARTINI,
            WatheItems.COSMOPOLITAN,
            WatheItems.CHAMPAGNE
    );

    // 随机食物池按需求列出，都是原版可直接食用物品。
    private static final List<Item> RANDOM_FOODS = List.of(
            Items.COOKED_BEEF,
            Items.COOKED_PORKCHOP,
            Items.COOKED_MUTTON,
            Items.COOKED_CHICKEN,
            Items.COOKED_RABBIT,
            Items.COOKED_SALMON,
            Items.APPLE,
            Items.BAKED_POTATO,
            Items.CARROT,
            Items.MELON_SLICE,
            Items.SWEET_BERRIES
    );

    private WaiterShopHandler() {
    }

    public static List<ShopEntry> getShopEntries() {
        List<ShopEntry> entries = new ArrayList<>();

        // 这里的添加顺序就是商店显示顺序；价格和 type 均来自需求。
        entries.add(new ShopEntry(ModItems.RANDOM_DRINK.getDefaultStack(), WaiterConstants.RANDOM_DRINK_PRICE, ShopEntry.Type.WEAPON));
        entries.add(new ShopEntry(ModItems.RANDOM_FOOD.getDefaultStack(), WaiterConstants.RANDOM_FOOD_PRICE, ShopEntry.Type.WEAPON));
        entries.add(new ShopEntry(ModItems.RANDOM_POTION.getDefaultStack(), WaiterConstants.RANDOM_POTION_PRICE, ShopEntry.Type.WEAPON));
        entries.add(new ShopEntry(WatheBlocks.BAR_STOOL.asItem().getDefaultStack(), WaiterConstants.BAR_STOOL_PRICE, ShopEntry.Type.POISON));
        entries.add(new ShopEntry(createFishingRodStack(), WaiterConstants.FISHING_ROD_PRICE, ShopEntry.Type.POISON));
        entries.add(new ShopEntry(Items.MUSIC_DISC_CREATOR.getDefaultStack(), WaiterConstants.MUSIC_DISC_PRICE, ShopEntry.Type.POISON));
        entries.add(new ShopEntry(Items.CAMPFIRE.getDefaultStack(), WaiterConstants.CAMPFIRE_PRICE, ShopEntry.Type.POISON));
        entries.add(new ShopEntry(Items.SMOKER.getDefaultStack(), WaiterConstants.SMOKER_PRICE, ShopEntry.Type.POISON));
        entries.add(new ShopEntry(ModItems.SLEEPING_BAG.getDefaultStack(), WaiterConstants.SLEEPING_BAG_PRICE, ShopEntry.Type.TOOL));
        entries.add(new ShopEntry(ModItems.BOOK.getDefaultStack(), WaiterConstants.BOOK_PRICE, ShopEntry.Type.TOOL));

        return entries;
    }

    public static ItemStack createRandomCocktailStack(Random random) {
        // 每次购买随机一种 Wathe 鸡尾酒，返回默认堆栈以保留原物品自己的使用逻辑。
        Item item = RANDOM_COCKTAILS.get(random.nextInt(RANDOM_COCKTAILS.size()));
        return item.getDefaultStack();
    }

    public static ItemStack createRandomFoodStack(Random random) {
        // 食物不做额外 NBT，服务员递予时会读取 FOOD 组件判断为“去吃点零食”。
        Item item = RANDOM_FOODS.get(random.nextInt(RANDOM_FOODS.size()));
        return item.getDefaultStack();
    }

    public static ItemStack createRandomPotionStack(Random random) {
        // 随机药水直接写 PotionContentsComponent，确保拿到的是真药水而不是商店图标。
        return switch (random.nextInt(WaiterConstants.RANDOM_POTION_VARIANTS)) {
            case 0 -> createPotionStack(new StatusEffectInstance(StatusEffects.REGENERATION, WaiterConstants.REGENERATION_DURATION_TICKS, WaiterConstants.STRONG_EFFECT_AMPLIFIER));
            case 1 -> createPotionStack(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, WaiterConstants.INSTANT_EFFECT_DURATION_TICKS, WaiterConstants.STRONG_EFFECT_AMPLIFIER));
            case 2 -> createPotionStack(new StatusEffectInstance(StatusEffects.STRENGTH, WaiterConstants.STRENGTH_DURATION_TICKS, WaiterConstants.STRONG_EFFECT_AMPLIFIER));
            case 3 -> createPotionStack(new StatusEffectInstance(StatusEffects.WATER_BREATHING, WaiterConstants.WATER_BREATHING_DURATION_TICKS, WaiterConstants.BASE_EFFECT_AMPLIFIER));
            default -> createPotionStack(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, WaiterConstants.FIRE_RESISTANCE_DURATION_TICKS, WaiterConstants.BASE_EFFECT_AMPLIFIER));
        };
    }

    public static void applyWaiterFishingRodDetails(ItemStack stack, ServerPlayerEntity player) {
        /*
         * 钓鱼竿的最大耐久可以在注册商店条目时写入，但“饵钓 5”需要通过玩家 registry 拿 EnchantmentEntry。
         * 所以购买交付阶段再补附魔，保证商店展示和玩家实际拿到的物品都满足需求。
         */
        stack.set(DataComponentTypes.MAX_DAMAGE, WaiterConstants.FISHING_ROD_MAX_DAMAGE);
        stack.set(DataComponentTypes.DAMAGE, 0);
        RegistryEntry<Enchantment> lure = player.getRegistryManager()
                .getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.LURE);
        stack.addEnchantment(lure, WaiterConstants.FISHING_ROD_LURE_LEVEL);
    }

    private static ItemStack createFishingRodStack() {
        // 商店列表里的预览堆栈先写 1 耐久；真正给玩家时再通过 applyWaiterFishingRodDetails 补附魔。
        ItemStack stack = Items.FISHING_ROD.getDefaultStack();
        stack.set(DataComponentTypes.MAX_DAMAGE, WaiterConstants.FISHING_ROD_MAX_DAMAGE);
        stack.set(DataComponentTypes.DAMAGE, 0);
        return stack;
    }

    private static ItemStack createPotionStack(StatusEffectInstance effect) {
        // Optional.empty() 表示不绑定原版预设药水，效果列表完全由这里传入的 StatusEffectInstance 决定。
        ItemStack stack = Items.POTION.getDefaultStack();
        stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.empty(), List.of(effect)));
        return stack;
    }
}
