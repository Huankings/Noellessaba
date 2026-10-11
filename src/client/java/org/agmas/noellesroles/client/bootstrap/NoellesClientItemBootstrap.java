package org.agmas.noellesroles.client.bootstrap;

import dev.doctor4t.wathe.api.client.tooltip.ItemTooltipApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.client.items.NoellesRolesItemExtraModel;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.hacker.HackerPhoneComponent;
import org.agmas.noellesroles.roles.lich.LichConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperPlayerComponent;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchMode;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchState;
import org.agmas.noellesroles.roles.waiter.WaiterConstants;

/**
 * 客户端物品注册入口。
 *
 * <p>颜色、Tooltip 和模型谓词都属于物品显示职责，集中在这里编排，
 * 但不再由客户端 Fabric 主入口直接维护数百行物品清单。</p>
 */
public final class NoellesClientItemBootstrap {
    private NoellesClientItemBootstrap() {
    }

    public static void register() {
        registerColors();
        registerTooltips();
        registerModels();
    }

    private static void registerColors() {
        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> tintIndex == 0 ? WaiterConstants.REGENERATION_POTION_COLOR : -1,
                ModItems.RANDOM_POTION
        );
    }

    private static void registerTooltips() {
        ItemTooltipApi.registerItems(
                ModItems.TOOLBOX, ModItems.CAPTURE_DEVICE, ModItems.POWER_RESTORATION,
                ModItems.FAKE_KNIFE, ModItems.FAKE_GRENADE, ModItems.FAKE_REVOLVER, ModItems.MASTER_KEY,
                ModItems.DELUSION_VIAL, ModItems.WIND_MARK, ModItems.DEFENSE_VIAL, ModItems.SEDATIVE,
                ModItems.ROLE_MINE, ModItems.TIMED_BOMB, ModItems.THROWING_AXE, ModItems.THROWING_PAN,
                ModItems.BLOOD_AXE, ModItems.COLORFUL_AXE, ModItems.THROWING_SPEED_AXE,
                ModItems.THROWING_BOMB_AXE, ModItems.SPRING_TRAP, ModItems.SPRING_TRAP_ADDTIME,
                ModItems.THROWING_BLOOD_AXE, ModItems.THROWING_MACHETE, ModItems.TOMAHAWK,
                ModItems.THROWING_TOYS_AXE, ModItems.THROWING_PICKAXE, ModItems.THROWING_JERRY_CAN,
                ModItems.ONCE_LIGHTER, ModItems.RANDOM_THROWING_WEAPON, ModItems.PSYCHO_JASON,
                ModItems.PSYCHO_COOK, ModItems.CRYSTAL_BALL, ModItems.ROBBER_PISTOL,
                ModItems.BOUNTY_PISTOL, ModItems.BOUNTY_DERRINGER, ModItems.BOUNTY_MODE, ModItems.BAYONET,
                ModItems.SILENCED_REVOLVER, ModItems.SILENT_GRENADE, ModItems.SNIPER_RIFLE,
                ModItems.SNIPER_RIFLE_BULLET, ModItems.BAYONET_COLDOWN_REFRESH, ModItems.DREAM_IMPRINT,
                ModItems.MEDICAL_KIT, ModItems.PAN, ModItems.PSYCHO_THROWING_PAN, ModItems.PILL,
                ModItems.TAPE, ModItems.HUNTING_KNIFE, ModItems.SULFURIC_ACID_BARREL, ModItems.BLOWGUN,
                ModItems.POISON_INJECTOR, ModItems.DELUSION_SYRINGE, ModItems.KNOCKOUT_DRUG,
                ModItems.JERRY_CAN, ModItems.LIGHTER, ModItems.ONCE_STAFF, ModItems.PSYCHO_STAFF,
                ModItems.MAGIC_BARRIER, ModItems.PSYCHO_LICH, ModItems.PHONE,
                ModItems.ICON_WEAPON_COOLDOWN_REFRESH, ModItems.ICON_ABILITY_COOLDOWN_REFRESH,
                ModItems.ICON_POTION_EFFECT_REFRESH, ModItems.DYING_WATCH_PROTECT, ModItems.SLEEPING_BAG,
                ModItems.BOOK, ModItems.RANDOM_FOOD, ModItems.RANDOM_DRINK, ModItems.RANDOM_POTION,
                ModItems.PSYCHO_VECNA, ModItems.VECNA_ADDTIME, ModItems.BUTCHER_KNIFE, ModItems.EVIL_POSSESS
        );
        ItemTooltipApi.registerAppender(
                NoellesRolesCore.id("psycho_staff_attack_speed_tooltip"),
                ItemTooltipApi.DEFAULT_PRIORITY,
                ModItems.PSYCHO_STAFF,
                context -> context.tooltip().add(Text.translatable(
                        "item.noellesroles.psycho_staff.tooltip.attack_speed",
                        LichConstants.PSYCHO_STAFF_ATTACK_SPEED
                ).setStyle(Style.EMPTY.withColor(ItemTooltipApi.REGULAR_TOOLTIP_COLOR)))
        );
        registerTimekeeperWatchTooltip();
    }

    private static void registerTimekeeperWatchTooltip() {
        ItemTooltipApi.registerAppender(
                NoellesRolesCore.id("timekeeper_watch_tooltip"),
                ItemTooltipApi.DEFAULT_PRIORITY,
                ModItems.DYING_WATCH,
                context -> {
                    TimekeeperWatchState state = TimekeeperWatchItem.getState(context.stack());
                    TimekeeperWatchMode mode = TimekeeperWatchItem.getMode(context.stack());
                    Style roleStyle = Style.EMPTY.withColor(TextColor.fromRgb(TimekeeperConstants.ROLE_COLOR));
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.state", state.text()).setStyle(roleStyle));
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.mode", mode.text()).setStyle(roleStyle));
                    MutableText cost = Text.translatable("item.noellesroles.dying_watch.tooltip.cost", mode.getTimeCost())
                            .setStyle(roleStyle)
                            .append(Text.literal("§f" + TimekeeperConstants.TIME_CURRENCY_ICON + "§r"));
                    context.tooltip().add(cost);
                    Text cooldownStatus = Text.translatable("item.noellesroles.dying_watch.tooltip.cooldown.ready");
                    if (context.player() != null) {
                        int cooldownTicks = TimekeeperPlayerComponent.KEY.get(context.player()).getCooldownTicks(mode);
                        if (cooldownTicks > 0) {
                            cooldownStatus = Text.translatable("item.noellesroles.dying_watch.tooltip.cooldown.seconds", Math.max(1, (cooldownTicks + 19) / 20));
                        }
                    }
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.cooldown", cooldownStatus).setStyle(roleStyle));
                    context.tooltip().add(Text.empty());
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.description").setStyle(roleStyle));
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.item_accelerate").setStyle(roleStyle));
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.ability_accelerate").setStyle(roleStyle));
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.rewind").setStyle(roleStyle));
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.breaks").setStyle(roleStyle));
                    context.tooltip().add(Text.translatable("item.noellesroles.dying_watch.tooltip.elegant").setStyle(roleStyle));
                }
        );
    }

    private static void registerModels() {
        Item[] items = {
                ModItems.TOOLBOX, ModItems.CAPTURE_DEVICE, ModItems.POWER_RESTORATION, ModItems.FAKE_KNIFE,
                ModItems.FAKE_GRENADE, ModItems.FAKE_REVOLVER, ModItems.MASTER_KEY, ModItems.DELUSION_VIAL,
                ModItems.WIND_MARK, ModItems.DEFENSE_VIAL, ModItems.SEDATIVE, ModItems.ROLE_MINE, ModItems.TIMED_BOMB,
                ModItems.THROWING_AXE, ModItems.THROWING_PAN, ModItems.BLOOD_AXE, ModItems.COLORFUL_AXE,
                ModItems.THROWING_SPEED_AXE, ModItems.THROWING_BOMB_AXE, ModItems.SPRING_TRAP, ModItems.SPRING_TRAP_ADDTIME,
                ModItems.THROWING_BLOOD_AXE, ModItems.THROWING_MACHETE, ModItems.TOMAHAWK, ModItems.THROWING_TOYS_AXE,
                ModItems.THROWING_PICKAXE, ModItems.ONCE_LIGHTER, ModItems.RANDOM_THROWING_WEAPON, ModItems.PSYCHO_JASON,
                ModItems.PSYCHO_COOK, ModItems.CRYSTAL_BALL, ModItems.ROBBER_PISTOL, ModItems.BOUNTY_PISTOL,
                ModItems.BOUNTY_DERRINGER, ModItems.BOUNTY_MODE, ModItems.BAYONET, ModItems.SILENCED_REVOLVER,
                ModItems.SILENT_GRENADE, ModItems.SNIPER_RIFLE, ModItems.SNIPER_RIFLE_BULLET, ModItems.DREAM_IMPRINT,
                ModItems.MEDICAL_KIT, ModItems.PAN, ModItems.PSYCHO_THROWING_PAN, ModItems.PILL, ModItems.TAPE,
                ModItems.HUNTING_KNIFE, ModItems.SULFURIC_ACID_BARREL, ModItems.BLOWGUN, ModItems.POISON_INJECTOR,
                ModItems.DELUSION_SYRINGE, ModItems.KNOCKOUT_DRUG, ModItems.JERRY_CAN, ModItems.LIGHTER,
                ModItems.ONCE_STAFF, ModItems.PSYCHO_STAFF, ModItems.MAGIC_BARRIER, ModItems.PSYCHO_LICH,
                ModItems.ICON_WEAPON_COOLDOWN_REFRESH, ModItems.ICON_ABILITY_COOLDOWN_REFRESH, ModItems.ICON_POTION_EFFECT_REFRESH,
                ModItems.DYING_WATCH_PROTECT, ModItems.SLEEPING_BAG, ModItems.BOOK, ModItems.RANDOM_FOOD,
                ModItems.RANDOM_DRINK, ModItems.RANDOM_POTION
        };
        for (Item item : items) {
            NoellesRolesItemExtraModel.registerExtraModel(item);
        }
        NoellesRolesItemExtraModel.registerJasonJerryCanModel(ModItems.THROWING_JERRY_CAN);
        NoellesRolesItemExtraModel.registerPhoneModel(ModItems.PHONE);
        NoellesRolesItemExtraModel.registerTimekeeperWatchModel(ModItems.DYING_WATCH);
    }
}
