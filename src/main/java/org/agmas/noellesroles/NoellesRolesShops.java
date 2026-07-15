package org.agmas.noellesroles;

import dev.doctor4t.wathe.api.shop.ShopApi;
import dev.doctor4t.wathe.api.shop.ShopPurchaseContext;
import dev.doctor4t.wathe.api.shop.ShopPurchaseResult;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.ShopPurchaseTracker;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.agmas.noellesroles.roles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.roles.engineer.EngineerPlayerComponent;
import org.agmas.noellesroles.shop.PlayerShopComponentAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class NoellesRolesShops {

    private static final Map<Item, Integer> ITEM_PRICES = new HashMap<>();

    // 从 Wathe 原版商店中提取基础价格，方便扩展职业直接复用。
    static {
        for (ShopEntry entry : GameConstants.SHOP_ENTRIES) {
            ITEM_PRICES.put(entry.stack().getItem(), entry.price());
        }
    }

    /**
     * 获取某个物品在 Wathe 商店中的原始价格。
     * 如果 Wathe 后续调整了价格，这里会自动跟随更新。
     */
    public static int getItemPrice(Item item, int defaultValue) {
        return ShopApi.getDefaultPrice(item, ITEM_PRICES.getOrDefault(item, defaultValue));
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

        if (context.balance() < entry.price() || player.getItemCooldownManager().isCoolingDown(item)) {
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
        if (item == ModItems.POWER_RESTORATION || item == ModItems.BAYONET_COLDOWN_REFRESH) {
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
        if (balance >= price && !player.getItemCooldownManager().isCoolingDown(item)) {
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
            if (item != ModItems.POWER_RESTORATION && item != ModItems.BAYONET_COLDOWN_REFRESH) {
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

        return player.giveItemStack(stack.copy());
    }

    /**
     * 动态商店和静态商店统一使用的“成功购买后扣钱并同步”逻辑。
     */
    public static void completePurchase(@NotNull PlayerShopComponentAccessor shop, int price) {
        shop.noellesroles$setBalance(shop.noellesroles$getBalance() - price);
        shop.noellesroles$sync();
    }
}
