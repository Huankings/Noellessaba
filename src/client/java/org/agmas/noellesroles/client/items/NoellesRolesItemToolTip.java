package org.agmas.noellesroles.client.items;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.ratatouille.util.TextUtils;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.util.WatheItemTooltips;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.assassin.AssassinPlayerComponent;
import org.agmas.noellesroles.roles.bomber.BomberPlayerComponent;
import org.agmas.noellesroles.roles.bounty_hunter.BountyHunterPlayerComponent;
import org.agmas.noellesroles.roles.dreamer.DreamerConstants;
import org.agmas.noellesroles.roles.dreamer.DreamerKillerComponent;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerPlayerComponent;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.agmas.noellesroles.roles.hunter.HunterPlayerComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperConstants;
import org.agmas.noellesroles.roles.rememberer.RemembererPlayerComponent;
import org.agmas.noellesroles.roles.robber.RobberPlayerComponent;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapPlayerComponent;
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

        if (item == ModItems.BOUNTY_PISTOL) {
            /*
             * 赏金手枪有三种总时长：开局 30 秒、目标击杀 15 秒、失败/非目标 45 秒。
             * 冷却遮罩本身只给剩余比例，所以必须从赏金猎人组件读取这次冷却来源。
             */
            int displayedTicks = BountyHunterPlayerComponent.KEY.get(client.player).getDisplayedBountyPistolCooldownTotalTicks();
            return displayedTicks > 0 ? displayedTicks : getItemCooldownTicks(item);
        }

        if (item == ModItems.BLOWGUN || item == ModItems.POISON_INJECTOR) {
            /*
             * 制毒师两件道具开局只锁 30 秒，但普通使用后是 45 秒。
             * 这里读取服务端同步的“开局冷却来源”标记，确保 tooltip 秒数和物品冷却遮罩都按 30 秒流逝。
             */
            DrugmakerPlayerComponent drugmakerComponent = DrugmakerPlayerComponent.KEY.get(client.player);
            if (drugmakerComponent.isUsingStartCooldown(item)) {
                return DrugmakerConstants.START_COOLDOWN_TICKS;
            }
            return getItemCooldownTicks(item);
        }

        if (item == ModItems.DELUSION_SYRINGE) {
            /*
             * 幻觉注剂普通注射后冷却 45 秒，但梦者开局拿到时只先锁 30 秒。
             * 客户端只能从原版冷却管理器拿剩余比例，所以需要读取梦者组件同步来的来源标记。
             */
            DreamerKillerComponent dreamerComponent = DreamerKillerComponent.KEY.get(client.player);
            if (dreamerComponent.isUsingDelusionSyringeStartCooldown(item)) {
                return DreamerConstants.DELUSION_SYRINGE_START_COOLDOWN_TICKS;
            }
            return getItemCooldownTicks(item);
        }

        if (item == ModItems.KNOCKOUT_DRUG) {
            /*
             * 迷药同样有 30 秒开局冷却和 45 秒普通冷却两种来源。
             * 不单独判断的话，客户端只能用 45 秒总长乘以 30 秒冷却比例，倒计时就会显示偏大。
             */
            KidnapperComponent kidnapperComponent = KidnapperComponent.KEY.get(client.player);
            if (kidnapperComponent.isUsingStartCooldown(item)) {
                return KidnapperConstants.START_COOLDOWN_TICKS;
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

        if (item == ModItems.HUNTING_KNIFE) {
            HunterPlayerComponent hunter = HunterPlayerComponent.KEY.get(client.player);
            if (hunter.isUsingStartCooldown(item)) {
                return HunterConstants.HUNTING_KNIFE_START_COOLDOWN_TICKS;
            }
            if (hunter.knifeTicks > 0) {
                return GameConstants.getInTicks(0, hunter.knifeTicks / 10);
            }
            return getItemCooldownTicks(item);
        }

        if (item == ModItems.BLOOD_AXE) {
            /*
             * 血斧有 30 秒开局冷却和 45 秒暗杀后冷却两种总长。
             * 原版冷却管理器只给剩余比例，所以这里读取弹簧陷阱组件标记，
             * 确保开局冷却的 tooltip 不会错误地从 45 秒总长开始换算。
             */
            SpringTrapPlayerComponent springTrap = SpringTrapPlayerComponent.KEY.get(client.player);
            if (springTrap.isUsingStartCooldown(item)) {
                return SpringTrapConstants.BLOOD_AXE_START_COOLDOWN_TICKS;
            }
            return getItemCooldownTicks(item);
        }

        if (item != ModItems.TIMED_BOMB) {
            return getItemCooldownTicks(item);
        }

        // 非炸弹客手里出现的定时炸弹，只可能是滴滴声阶段的传递冷却
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(client.player.getWorld());
        if (!gameWorld.isRole(client.player, NoellesRoleRegistry.BOMBER)) {
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
                /*
                 * 旁观/创造/非存活玩家不应该被这些迁移物品的冷却表现限制。
                 * 这里隐藏 tooltip 倒计时，保持界面和服务端“调试身份不受冷却影响”的行为一致。
                 */
                if (isIgnoredForSpectatorOrCreative(item) && GameFunctions.isPlayerSpectatingOrCreative(MinecraftClient.getInstance().player)) {
                    return;
                }
                /*
                 * 猎刀松开“疾跑举刀”后会写入一段临时冷却。
                 * kinssaba 对这类冷却只显示“冷却中...”，避免把短暂、动态的两倍举刀时间误展示成固定物品冷却。
                 */
                if (item == ModItems.HUNTING_KNIFE && HunterPlayerComponent.KEY.get(MinecraftClient.getInstance().player).knifeTicks > 0) {
                    list.add(Text.translatable("tip.noellesroles.cooldown_temporary").withColor(WatheItemTooltips.COOLDOWN_COLOR));
                    return;
                }
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

    private static boolean isIgnoredForSpectatorOrCreative(@NotNull Item item) {
        return item == ModItems.HUNTING_KNIFE
                || item == ModItems.THROWING_PAN
                || item == ModItems.PSYCHO_THROWING_PAN
                || item == ModItems.PSYCHO_COOK
                || item == ModItems.BLOWGUN
                || item == ModItems.POISON_INJECTOR
                || item == ModItems.DELUSION_SYRINGE
                || item == ModItems.KNOCKOUT_DRUG
                || item == ModItems.JERRY_CAN
                || item == ModItems.LIGHTER;
    }
}
