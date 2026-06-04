package org.agmas.noellesroles.client.items;

import dev.doctor4t.ratatouille.util.TextUtils;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.util.WatheItemTooltips;
import dev.doctor4t.wathe.game.GameConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.rememberer.RemembererPlayerComponent;
import org.agmas.noellesroles.roles.robber.RobberPlayerComponent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoellesRolesItemToolTip {

    private static final Map<Item, Integer> presetCooldowns = new HashMap<>();

    public static int getItemCooldownTicks(@NotNull Item item) {
        return presetCooldowns.getOrDefault(item, 0);
    }

    /**
     * 计算物品当前这一次冷却对应的“总冷却时长”。
     * 大部分物品只有一种固定冷却，直接走预设值即可；
     * 但定时炸弹既有炸弹客开局冷却，也有真正拿在手里时的传递冷却，所以需要动态判断。
     */
    public static int getCurrentCooldownTicks(@NotNull Item item) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return getItemCooldownTicks(item);
        }

        if (item == ModItems.THROWING_AXE || item == ModItems.ROBBER_PISTOL) {
            RobberPlayerComponent robberComponent = RobberPlayerComponent.KEY.get(client.player);
            if (robberComponent.isUsingStartCooldown(item)) {
                return RobberPlayerComponent.ROBBER_START_COOLDOWN_TICKS;
            }
            return getItemCooldownTicks(item);
        }

        if (item == ModItems.BAYONET || item == ModItems.SILENCED_REVOLVER) {
            /*
             * 刺客这两件武器除了各自的常规冷却外，还额外拥有“开局先锁 30 秒”的职业冷却。
             * 这里必须识别当前冷却来源，才能让 tooltip 的剩余秒数按正确总时长去换算。
             */
            AssassinPlayerComponent assassinComponent = AssassinPlayerComponent.KEY.get(client.player);
            if (assassinComponent.isUsingStartCooldown(item)) {
                return AssassinPlayerComponent.ASSASSIN_START_COOLDOWN_TICKS;
            }
            return getItemCooldownTicks(item);
        }

        if (item == ModItems.SNIPER_RIFLE) {
            /*
             * 狙击枪有三种冷却来源：
             * 1. 开局 30 秒；
             * 2. 切回武器时的部署 2 秒；
             * 3. 开火后的 4 秒。
             *
             * 这里必须读追忆者组件同步过来的“当前来源”，
             * 否则 tooltip 只能拿到一个剩余比例，无法知道这次应该按哪种总时长换算秒数。
             */
            int displayedTicks = RemembererPlayerComponent.KEY.get(client.player).getDisplayedSniperCooldownTotalTicks();
            return displayedTicks > 0 ? displayedTicks : getItemCooldownTicks(item);
        }

        if (item != ModItems.TIMED_BOMB) {
            return getItemCooldownTicks(item);
        }

        // 非炸弹客手里出现的定时炸弹，只可能是滴滴声阶段的传递冷却
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, Noellesroles.BOMBER)) {
            return BomberPlayerComponent.TRANSFER_COOLDOWN_TICKS;
        }

        // 炸弹客本人如果身上正挂着活动炸弹，说明当前看到的是传递冷却；
        // 否则显示开局商店冷却。
        BomberPlayerComponent bomberComponent = BomberPlayerComponent.KEY.get(client.player);
        if (bomberComponent.hasBomb()) {
            return BomberPlayerComponent.TRANSFER_COOLDOWN_TICKS;
        }
        return BomberPlayerComponent.BOMBER_START_COOLDOWN_TICKS;
    }

    /**
     * 初始化预设冷却时间（直接从 GameConstants.ITEM_COOLDOWNS 复制所有物品的冷却）
     * 与 KinsWathe 实现一致
     */
    public static void initItemCooldown() {
        presetCooldowns.putAll(GameConstants.ITEM_COOLDOWNS);
    }

    /**
     * 为物品添加提示（描述 + 冷却）
     */
    public static void addItemtip(@NotNull Item item, @NotNull ItemStack itemStack, @NotNull List<Text> list) {
        if (itemStack.isOf(item)) {
            addCooldowntip(item, itemStack, list);
            addTooltip(item, itemStack, list);
        }
    }

    /**
     * 添加物品描述（从语言文件读取）
     */
    public static void addTooltip(@NotNull Item item, @NotNull ItemStack itemStack, @NotNull List<Text> list) {
        if (itemStack.isOf(item)) {
            list.addAll(TextUtils.getTooltipForItem(item, Style.EMPTY.withColor(WatheItemTooltips.REGULAR_TOOLTIP_COLOR)));
        }
    }

    /**
     * 添加冷却提示（显示剩余冷却时间）
     */
    public static void addCooldowntip(@NotNull Item item, @NotNull ItemStack itemStack, @NotNull List<Text> list) {
        if (MinecraftClient.getInstance().player == null) return;
        if (itemStack.isOf(item)) {
            initItemCooldown(); // 确保预设冷却已加载
            ItemCooldownManager itemCooldown = MinecraftClient.getInstance().player.getItemCooldownManager();
            if (itemCooldown != null && itemCooldown.isCoolingDown(item)) {
                float progress = itemCooldown.getCooldownProgress(item, 0);
                int totalTicks = getCurrentCooldownTicks(item);
                if (totalTicks > 0) {
                    int remainingTicks = (int) (totalTicks * progress) + 19;
                    int totalSeconds = remainingTicks / 20;
                    int minutes = totalSeconds / 60;
                    int seconds = totalSeconds % 60;
                    String countdown = (minutes > 0 ? minutes + "m" : "") + (seconds > 0 ? seconds + "s" : "");
                    list.add(Text.translatable("tip.cooldown", countdown).withColor(WatheItemTooltips.COOLDOWN_COLOR));
                }
            }
        }
    }
}
