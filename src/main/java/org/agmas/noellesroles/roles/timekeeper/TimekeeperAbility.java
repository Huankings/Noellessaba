package org.agmas.noellesroles.roles.timekeeper;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.TimekeeperWatchItem;
import org.agmas.noellesroles.mixin.roles.convener.ItemCooldownManagerAccessor;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 时停者主动逻辑。
 *
 * <p>怀表的左键模式切换在客户端入口里处理并同步到服务端；
 * 右键真正生效统一收口到这里。所有扣光阴、写冷却、发提示和回放记录都在服务端做，
 * 防止客户端伪造包或不同步状态导致免费发动技能。</p>
 */
public final class TimekeeperAbility {
    private TimekeeperAbility() {
    }

    public static boolean canStartRewindCharge(@NotNull ServerPlayerEntity player, @NotNull ItemStack stack) {
        return validateWatchUse(player, stack, TimekeeperWatchMode.REWIND, true);
    }

    public static boolean tryUseWatchMode(
            @NotNull ServerPlayerEntity player,
            @NotNull ItemStack stack,
            @NotNull TimekeeperWatchMode mode
    ) {
        if (!validateWatchUse(player, stack, mode, false)) {
            return false;
        }

        TimekeeperWatchState state = TimekeeperWatchItem.getState(stack);
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        TimekeeperPlayerComponent component = TimekeeperPlayerComponent.KEY.get(player);

        int cost = mode.getTimeCost();
        shop.addCurrencyAmount(TimekeeperConstants.TIME_CURRENCY_ID, -cost);

        boolean success = switch (mode) {
            case ITEM_ACCELERATE -> refreshItemCooldowns(player);
            case ABILITY_ACCELERATE -> refreshAbilityCooldowns(player);
            case REWIND -> TimekeeperWorldComponent.KEY.get(player.getServerWorld()).tryStartRewind(player, state.isElegant());
        };

        if (!success) {
            shop.addCurrencyAmount(TimekeeperConstants.TIME_CURRENCY_ID, cost);
            return false;
        }

        component.setCooldown(mode, getCooldownForMode(mode, state));
        player.playSoundToPlayer(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.1f);
        TimekeeperReplayHelper.recordWatchUse(player, stack, mode, cost);

        if (mode == TimekeeperWatchMode.REWIND && !state.isElegant()) {
            /*
             * 普通濒毁怀表成功发动时间回溯后必定破碎。
             * 这里放在回溯启动成功之后，而不是蓄力开始时，
             * 确保“光阴不足 / 冷却中 / 当前已经回溯中”这些失败情况不会误损坏怀表。
             */
            TimekeeperWatchItem.setState(stack, TimekeeperWatchState.BROKEN);
            TimekeeperReplayHelper.recordWatchBroken(player, stack);
        }

        if (mode == TimekeeperWatchMode.REWIND) {
            /*
             * 回溯会把玩家背包和时停者个人组件倒回 30 秒前。
             * 但“发动本次回溯”产生的代价不能被回滚掉：光阴已经支付，冷却已经写入，
             * 普通濒毁怀表也可能已经破碎。
             * 因此所有后置代价完成后，再把发动者当前状态交给世界组件保留；
             * 回放播放每应用一张旧快照，世界组件都会把这份后置状态重新压回去。
             */
            TimekeeperWorldComponent.KEY.get(player.getServerWorld()).rememberActorPostUseState(player, stack);
        }

        return true;
    }

    /**
     * 能力键：优先升级普通怀表，再修复损坏怀表。
     *
     * <p>用户特别提到“背包里出现两个不一样状态怀表时优先升级再修复”，
     * 所以这里先找 NORMAL，再找 BROKEN。HUD 也会走同样顺序，保证提示和真实行为一致。</p>
     */
    public static void handleRepairOrUpgrade(@NotNull ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, NoellesRoleRegistry.TIMEKEEPER) || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        ItemStack normalWatch = findWatch(player, TimekeeperWatchState.NORMAL);
        if (!normalWatch.isEmpty()) {
            tryUpgradeWatch(player, normalWatch);
            return;
        }

        ItemStack brokenWatch = findWatch(player, TimekeeperWatchState.BROKEN);
        if (!brokenWatch.isEmpty()) {
            tryRepairWatch(player, brokenWatch);
            return;
        }

        sendActionbar(player, Text.translatable("tip.noellesroles.timekeeper.no_watch"));
    }

    public static void switchWatchMode(@NotNull ServerPlayerEntity player, int modeOrdinal) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        ItemStack stack = player.getMainHandStack();
        if (!gameWorld.isRole(player, NoellesRoleRegistry.TIMEKEEPER)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !stack.isOf(ModItems.DYING_WATCH)) {
            return;
        }

        TimekeeperWatchMode[] modes = TimekeeperWatchMode.values();
        TimekeeperWatchMode mode = modeOrdinal >= 0 && modeOrdinal < modes.length
                ? modes[modeOrdinal]
                : TimekeeperWatchItem.getMode(stack).next();
        TimekeeperWatchItem.setMode(stack, mode);
        sendActionbar(player, Text.translatable("message.noellesroles.timekeeper.current_watch_mode", mode.text()));
    }

    public static void sendActionbar(@NotNull ServerPlayerEntity player, @NotNull Text text) {
        player.sendMessage(text.copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(TimekeeperConstants.ROLE_COLOR))), true);
    }

    private static boolean validateWatchUse(
            @NotNull ServerPlayerEntity player,
            @NotNull ItemStack stack,
            @NotNull TimekeeperWatchMode mode,
            boolean chargingOnly
    ) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(player, NoellesRoleRegistry.TIMEKEEPER)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !stack.isOf(ModItems.DYING_WATCH)) {
            return false;
        }

        if (TimekeeperWatchItem.getState(stack).isBroken()) {
            sendActionbar(player, Text.translatable("message.noellesroles.timekeeper.watch_broken"));
            player.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return false;
        }

        TimekeeperPlayerComponent component = TimekeeperPlayerComponent.KEY.get(player);
        int cooldown = component.getCooldownTicks(mode);
        if (cooldown > 0) {
            sendActionbar(player, Text.translatable("message.noellesroles.timekeeper.watch_cooldown", (cooldown + 19) / 20));
            player.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return false;
        }

        TimekeeperWorldComponent worldComponent = TimekeeperWorldComponent.KEY.get(player.getServerWorld());
        if (mode == TimekeeperWatchMode.REWIND && !worldComponent.canStartRewind(player)) {
            sendActionbar(player, Text.translatable("message.noellesroles.timekeeper.rewind_blocked"));
            player.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return false;
        }

        int balance = PlayerShopComponent.KEY.get(player).getCurrencyAmount(TimekeeperConstants.TIME_CURRENCY_ID);
        if (balance < mode.getTimeCost()) {
            sendActionbar(player, Text.translatable("message.noellesroles.timekeeper.not_enough_time"));
            player.playSoundToPlayer(SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return false;
        }

        return true;
    }

    private static boolean refreshItemCooldowns(@NotNull ServerPlayerEntity timekeeper) {
        List<ServerPlayerEntity> targets = eligibleGoodSideTargets(timekeeper);
        for (ServerPlayerEntity target : targets) {
            clearItemCooldowns(target);
            sendActionbar(target, Text.translatable("message.noellesroles.timekeeper.item_cooldown_refreshed"));
        }
        return true;
    }

    private static boolean refreshAbilityCooldowns(@NotNull ServerPlayerEntity timekeeper) {
        List<ServerPlayerEntity> targets = eligibleGoodSideTargets(timekeeper);
        for (ServerPlayerEntity target : targets) {
            AbilityPlayerComponent.KEY.get(target).setCooldown(0);
            sendActionbar(target, Text.translatable("message.noellesroles.timekeeper.ability_cooldown_refreshed"));
        }
        return true;
    }

    private static List<ServerPlayerEntity> eligibleGoodSideTargets(@NotNull ServerPlayerEntity timekeeper) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(timekeeper.getWorld());
        List<ServerPlayerEntity> targets = new ArrayList<>();
        for (ServerPlayerEntity target : timekeeper.getServerWorld().getPlayers()) {
            if (target.getUuid().equals(timekeeper.getUuid()) || !GameFunctions.isPlayerAliveAndSurvival(target)) {
                continue;
            }
            Role role = gameWorld.getRole(target);
            if (role == null) {
                continue;
            }
            Faction faction = role.getFaction();
            if (faction == Faction.CIVILIAN || faction == Faction.VIGILANTE) {
                targets.add(target);
            }
        }
        return targets;
    }

    private static void clearItemCooldowns(@NotNull ServerPlayerEntity player) {
        ItemCooldownManager cooldownManager = player.getItemCooldownManager();
        List<Item> coolingItems = new ArrayList<>(((ItemCooldownManagerAccessor) (Object) cooldownManager).noellesroles$getEntries().keySet());
        for (Item item : coolingItems) {
            cooldownManager.remove(item);
        }
    }

    private static int getCooldownForMode(@NotNull TimekeeperWatchMode mode, @NotNull TimekeeperWatchState state) {
        return switch (mode) {
            case ITEM_ACCELERATE, ABILITY_ACCELERATE -> state.isElegant()
                    ? TimekeeperConstants.ELEGANT_ACCELERATE_COOLDOWN_TICKS
                    : TimekeeperConstants.NORMAL_ACCELERATE_COOLDOWN_TICKS;
            case REWIND -> state.isElegant()
                    ? TimekeeperConstants.ELEGANT_REWIND_COOLDOWN_TICKS
                    : TimekeeperConstants.NORMAL_REWIND_COOLDOWN_TICKS;
        };
    }

    private static void tryRepairWatch(@NotNull ServerPlayerEntity player, @NotNull ItemStack watchStack) {
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.balance < TimekeeperConstants.REPAIR_WATCH_PRICE) {
            sendActionbar(player, Text.translatable("tip.noellesroles.timekeeper.repair.need_money", TimekeeperConstants.REPAIR_WATCH_PRICE));
            return;
        }

        shop.addToBalance(-TimekeeperConstants.REPAIR_WATCH_PRICE);
        TimekeeperWatchItem.setState(watchStack, TimekeeperWatchState.NORMAL);
        TimekeeperReplayHelper.recordWatchRepair(player, watchStack, TimekeeperConstants.REPAIR_WATCH_PRICE);
        player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.8f, 1.3f);
    }

    private static void tryUpgradeWatch(@NotNull ServerPlayerEntity player, @NotNull ItemStack watchStack) {
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.balance < TimekeeperConstants.UPGRADE_WATCH_PRICE) {
            sendActionbar(player, Text.translatable("tip.noellesroles.timekeeper.upgrade.need_money", TimekeeperConstants.UPGRADE_WATCH_PRICE));
            return;
        }

        ItemStack before = watchStack.copy();
        shop.addToBalance(-TimekeeperConstants.UPGRADE_WATCH_PRICE);
        TimekeeperWatchItem.setState(watchStack, TimekeeperWatchState.ELEGANT);
        TimekeeperReplayHelper.recordWatchUpgrade(player, before, watchStack, TimekeeperConstants.UPGRADE_WATCH_PRICE);
        player.playSoundToPlayer(SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.8f, 1.6f);
    }

    private static ItemStack findWatch(@NotNull ServerPlayerEntity player, @NotNull TimekeeperWatchState state) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.DYING_WATCH) && TimekeeperWatchItem.getState(stack) == state) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
