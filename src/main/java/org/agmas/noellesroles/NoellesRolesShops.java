package org.agmas.noellesroles;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.ShopPurchaseTracker;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
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
        return ITEM_PRICES.getOrDefault(item, defaultValue);
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
            boolean success;

            // 特殊道具需要在购买瞬间直接触发效果，而不是塞进背包里。
            if (item == WatheItems.BLACKOUT) {
                success = PlayerShopComponent.useBlackout(player);
            } else if (item == WatheItems.PSYCHO_MODE) {
                success = PlayerShopComponent.usePsychoMode(player);
            } else if (item == ModItems.POWER_RESTORATION) {
                success = EngineerPlayerComponent.tryRestorePower(player);
            } else if (item == ModItems.BAYONET_COLDOWN_REFRESH) {
                /*
                 * 刺刀冷却刷新是“即时生效图标”，
                 * 购买成功与否不取决于背包空间，而取决于刺刀当前是否真的在冷却。
                 */
                success = AssassinPlayerComponent.tryRefreshBayonetCooldown(player);
            } else {
                success = player.giveItemStack(stack.copy());
            }

            if (success) {
                /*
                 * NoellesRoles 的商店内容会替换掉原版固定格子，
                 * 因此这里要把“本次真实购买到的 stack”回填给 Wathe 回放系统，
                 * 避免后续仍按原版第几个格子去误报匕首 / 左轮等商品。
                 */
                ShopPurchaseTracker.captureSuccessfulPurchase(player, stack.copy(), -1, price);
                playBuySound(player);
                return true;
            }

            // 这两类即时道具都会各自给出更具体的失败原因，不再让通用提示覆盖。
            if (item != ModItems.POWER_RESTORATION && item != ModItems.BAYONET_COLDOWN_REFRESH) {
                player.sendMessage(Text.translatable("shop.purchase_failed").withColor(0xAA0000), true);
            }
        } else {
            player.sendMessage(Text.translatable("shop.purchase_failed").withColor(0xAA0000), true);
        }

        playFailSound(player);
        return false;
    }

    /**
     * 动态商店和静态商店统一使用的“成功购买后扣钱并同步”逻辑。
     */
    public static void completePurchase(@NotNull PlayerShopComponentAccessor shop, int price) {
        shop.noellesroles$setBalance(shop.noellesroles$getBalance() - price);
        shop.noellesroles$sync();
    }

    private static void playBuySound(@NotNull PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.playSoundToPlayer(
                    WatheSounds.UI_SHOP_BUY,
                    SoundCategory.PLAYERS,
                    1.0F,
                    0.9F + player.getRandom().nextFloat() * 0.2F
            );
        }
    }

    private static void playFailSound(@NotNull PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.playSoundToPlayer(
                    WatheSounds.UI_SHOP_BUY_FAIL,
                    SoundCategory.PLAYERS,
                    1.0F,
                    0.9F + player.getRandom().nextFloat() * 0.2F
            );
        }
    }
}
