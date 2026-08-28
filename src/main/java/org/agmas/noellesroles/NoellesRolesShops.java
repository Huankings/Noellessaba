package org.agmas.noellesroles;

import dev.doctor4t.wathe.api.shop.ShopApi;
import dev.doctor4t.wathe.api.economy.EconomyApi;
import dev.doctor4t.wathe.api.shop.ShopPurchaseContext;
import dev.doctor4t.wathe.api.shop.ShopPurchaseResult;
import dev.doctor4t.wathe.api.shop.ShopPrice;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.ShopPurchaseTracker;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.roles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.agmas.noellesroles.roles.cook.CookPsychoHandler;
import org.agmas.noellesroles.roles.engineer.EngineerPlayerComponent;
import org.agmas.noellesroles.roles.hacker.HackerComponent;
import org.agmas.noellesroles.roles.lich.LichPsychoHandler;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperShopHandler;
import org.agmas.noellesroles.roles.waiter.WaiterConstants;
import org.agmas.noellesroles.roles.waiter.WaiterShopHandler;
import org.agmas.noellesroles.shop.PlayerShopComponentAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class NoellesRolesShops {

    private static final Map<Item, Integer> ITEM_PRICES = new HashMap<>();
    private static final Map<Item, ShopPrice> ITEM_SHOP_PRICES = new HashMap<>();

    // 从 Wathe 原版商店中提取基础价格，方便扩展职业直接复用。
    static {
        for (ShopEntry entry : GameConstants.SHOP_ENTRIES) {
            ITEM_SHOP_PRICES.put(entry.stack().getItem(), entry.shopPrice());
            ITEM_PRICES.put(entry.stack().getItem(), entry.price());
        }
    }

    /**
     * 获取某个物品在 Wathe 商店中的原始价格。
     * 如果 Wathe 后续调整了价格，这里会自动跟随更新。
     *
     * <p>这里固定读取默认商店第 0 组支付方案里的金币价格。
     * 如果需要任务币或疯魔模式第 1 组价格，请使用 {@link #getItemCurrencyPrice(Item, int, Identifier, int)}
     * 明确指定“哪一组支付方案 + 哪一种货币”。</p>
     */
    public static int getItemPrice(Item item, int defaultValue) {
        return getItemCurrencyPrice(item, 0, EconomyApi.MONEY, ITEM_PRICES.getOrDefault(item, defaultValue));
    }

    /**
     * 按“支付方案索引 + 货币 id”读取 Wathe 默认商店价格。
     *
     * <p>这样扩展职业可以只取自己需要的那一部分价格，
     * 不会因为默认商店里存在任务币或多方案支付，就把整套价格条件误复制过去。</p>
     */
    public static int getItemCurrencyPrice(Item item, int optionIndex, Identifier currency, int defaultValue) {
        return ShopApi.getDefaultCurrencyPrice(item, optionIndex, currency, defaultValue);
    }

    /**
     * 读取 Wathe 默认商店里某个物品的完整价格定义。
     *
     * <p>这里返回的是原始 {@link ShopPrice}，而不是旧的单个金币数值。
     * 这样外部扩展如果想区分“金币 / 任务币 / 多方案 OR 价格”，就可以直接读取完整结构，
     * 不会被旧版 int 兼容层截断。只有明确想完整继承默认杀手商品价格时才应使用它；
     * 普通职业商店请优先用 {@link #getItemCurrencyPrice(Item, int, Identifier, int)} 拆开读取。</p>
     */
    public static ShopPrice getItemShopPrice(Item item) {
        return ITEM_SHOP_PRICES.get(item);
    }

    /**
     * 提供给 Wathe ShopApi 的 NoellesRoles 购买交付逻辑。
     *
     * <p>这里和旧 mixin 最大的区别是：本方法只判断“商品是否真的交付成功”，
     * 不再扣钱、不再播放购买音效、不再写回放记录。那些公共副作用全部由 Wathe
     * 的 {@code PlayerShopComponent#tryBuy} 统一处理，避免多个扩展各自复制流程。</p>
     */
    public static @NotNull ShopPurchaseResult purchase(@NotNull ShopPurchaseContext context) {
        PlayerEntity player = context.player();
        ShopEntry entry = context.entry();
        Item item = entry.stack().getItem();

        /*
         * 这里改为读取 entry 自己的 ShopPrice，而不是只看 legacy 金币余额。
         * 这样 NoellesRoles 的职业商店以后如果也开始使用任务币或多方案价格，
         * 购买判定会自动生效，不会被旧的 int 金额逻辑截断。
         */
        if (!context.canAffordEntry() || (!ignoresShopCooldown(player, item) && player.getItemCooldownManager().isCoolingDown(item))) {
            return ShopPurchaseResult.FAIL_SHOW_MESSAGE;
        }

        boolean success = deliverPurchasedStack(player, entry.stack());
        if (success) {
            return ShopPurchaseResult.SUCCESS;
        }

        /*
         * 这两类即时道具会在自己的逻辑里发送更具体的失败原因：
         * 例如“当前没有停电”或“刺刀没有处于冷却”。Wathe 仍会播放失败音效，
         * 但不再额外显示通用的“购买失败”覆盖细节。
         */
        if (item == ModItems.POWER_RESTORATION
                || item == ModItems.BAYONET_COLDOWN_REFRESH
                || item == ModItems.DYING_WATCH_PROTECT) {
            return ShopPurchaseResult.FAIL_SILENT;
        }
        return ShopPurchaseResult.FAIL_SHOW_MESSAGE;
    }

    /**
     * 统一处理 noellesroles 自定义商店购买逻辑。
     * 这里会额外处理：
     * 1. Wathe 的特殊功能道具（停电、疯魔模式）
     * 2. 工程师的电力恢复系统
     * 3. 多数量堆叠道具（例如 4 个便条）
     */
    public static boolean handlePurchase(@NotNull PlayerEntity player, int balance, @NotNull ItemStack stack, int price) {
        Item item = stack.getItem();
        if (balance >= price && (ignoresShopCooldown(player, item) || !player.getItemCooldownManager().isCoolingDown(item))) {
            boolean success = deliverPurchasedStack(player, stack);

            if (success) {
                /*
                 * NoellesRoles 的商店内容会替换掉原版固定格子，
                 * 因此这里要把“本次真实购买到的 stack”回填给 Wathe 回放系统，
                 * 避免后续仍按原版第几个格子去误报匕首 / 左轮等商品。
                 */
                ShopPurchaseTracker.captureSuccessfulPurchase(player, stack.copy(), -1, price);
                ShopApi.playBuySound(player);
                return true;
            }

            // 这两类即时道具都会各自给出更具体的失败原因，不再让通用提示覆盖。
            if (item != ModItems.POWER_RESTORATION
                    && item != ModItems.BAYONET_COLDOWN_REFRESH
                    && item != ModItems.DYING_WATCH_PROTECT) {
                ShopApi.sendPurchaseFailedMessage(player);
            }
        } else {
            ShopApi.sendPurchaseFailedMessage(player);
        }

        ShopApi.playFailSound(player);
        return false;
    }

    private static boolean deliverPurchasedStack(@NotNull PlayerEntity player, @NotNull ItemStack stack) {
        Item item = stack.getItem();

        // 特殊道具需要在购买瞬间直接触发效果，而不是塞进背包里。
        if (item == WatheItems.BLACKOUT) {
            return PlayerShopComponent.useBlackout(player);
        }
        if (item == WatheItems.PSYCHO_MODE) {
            return PlayerShopComponent.usePsychoMode(player);
        }
        if (item == ModItems.BOUNTY_MODE) {
            /*
             * 赏金模式是商店即时按钮：购买成功后直接给模式德林加并开启疯魔外观/护盾，
             * 不把图标物品放进背包，避免玩家把“按钮”当作普通物品移动或丢弃。
             */
            return BountyHunterPlayerComponent.KEY.get(player).tryStartBountyMode();
        }
        if (item == ModItems.PSYCHO_COOK) {
            /*
             * 厨师疯魔是即时商店按钮：
             * 购买成功后直接启动 Wathe psycho profile 并授予疯魔飞锅，不把图标塞进背包。
             */
            return CookPsychoHandler.startCookPsycho(player);
        }
        if (item == ModItems.PSYCHO_LICH) {
            /*
             * 巫妖疯魔同样是即时商店按钮：购买成功后启动巫妖 profile 并授予疯魔法杖。
             * 不把图标塞进背包，避免玩家把商店按钮移动、丢弃或带出疯魔流程。
             */
            return LichPsychoHandler.startLichPsycho(player);
        }
        if (item == ModItems.POWER_RESTORATION) {
            return EngineerPlayerComponent.tryRestorePower(player);
        }
        if (item == ModItems.BAYONET_COLDOWN_REFRESH) {
            /*
             * 刺刀冷却刷新是“即时生效图标”，购买成功与否取决于刺刀是否真的在冷却，
             * 而不是玩家背包有没有空位。
             */
            return AssassinPlayerComponent.tryRefreshBayonetCooldown(player);
        }
        if (item == ModItems.ICON_WEAPON_COOLDOWN_REFRESH) {
            return HackerComponent.refreshWeaponCooldown(player);
        }
        if (item == ModItems.ICON_ABILITY_COOLDOWN_REFRESH) {
            return HackerComponent.refreshAbilityCooldown(player);
        }
        if (item == ModItems.ICON_POTION_EFFECT_REFRESH) {
            return HackerComponent.refreshPotionEffect(player);
        }
        if (item == ModItems.DYING_WATCH_PROTECT) {
            /*
             * 回溯保护是“购买后做标记”的商店按钮，不应该把图标物品塞进背包。
             * 是否允许购买由时停者自己的 handler 判断，Wathe 负责统一扣金币和播放购买/失败音效。
             */
            return TimekeeperShopHandler.tryBuyRewindProtection(player);
        }
        if (item == ModItems.RANDOM_DRINK) {
            return player.giveItemStack(WaiterShopHandler.createRandomCocktailStack(player.getRandom()));
        }
        if (item == ModItems.RANDOM_FOOD) {
            return player.giveItemStack(WaiterShopHandler.createRandomFoodStack(player.getRandom()));
        }
        if (item == ModItems.RANDOM_POTION) {
            return player.giveItemStack(WaiterShopHandler.createRandomPotionStack(player.getRandom()));
        }

        // 普通购买默认直接交付原物品；只有 waiter 的图标类商品会在上面几个分支里被替换成随机实物。
        ItemStack deliveredStack = stack.copy();
        if (item == Items.FISHING_ROD
                && player instanceof ServerPlayerEntity serverPlayer
                && deliveredStack.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0) == WaiterConstants.FISHING_ROD_MAX_DAMAGE) {
            // 服务员商店钓鱼竿要求“最大耐久 1 + 饵钓 5”，所以在购买结算时补写附魔。
            WaiterShopHandler.applyWaiterFishingRodDetails(deliveredStack, serverPlayer);
        }

        return player.giveItemStack(deliveredStack);
    }

    private static boolean ignoresShopCooldown(@NotNull PlayerEntity player, @NotNull Item item) {
        /*
         * 用户要求新物品方便调试：旁观/创造调试玩家不受冷却影响。
         * 这里只对已明确要求的调试友好物品开放商店冷却绕过，避免影响其它既有职业商品的正式平衡。
         */
        return (item == ModItems.PSYCHO_COOK
                || item == ModItems.ONCE_STAFF
                || item == ModItems.PSYCHO_STAFF
                || item == ModItems.MAGIC_BARRIER
                || item == ModItems.PSYCHO_LICH)
                && GameFunctions.isPlayerSpectatingOrCreative(player);
    }

    /**
     * 动态商店和静态商店统一使用的“成功购买后扣钱并同步”逻辑。
     */
    public static void completePurchase(@NotNull PlayerShopComponentAccessor shop, int price) {
        shop.noellesroles$setBalance(shop.noellesroles$getBalance() - price);
        shop.noellesroles$sync();
    }
}
