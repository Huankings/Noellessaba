package org.agmas.noellesroles.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperAbility;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperConstants;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchMode;
import org.agmas.noellesroles.roles.timekeeper.TimekeeperWatchState;
import org.jetbrains.annotations.NotNull;

/**
 * 时停者的濒毁怀表。
 *
 * <p>怀表只有一个物品 id，但通过数据组件保存“状态”和“模式”。
 * 这样玩家丢弃、商店检查、快照回滚背包时都只需要跟踪一个物品类型，
 * 精致怀表和损坏怀表只是同一件物品的不同运行态。</p>
 */
public class TimekeeperWatchItem extends Item {
    public TimekeeperWatchItem(Settings settings) {
        super(settings);
    }

    public static @NotNull ItemStack createStack(TimekeeperWatchState state) {
        ItemStack stack = ModItems.DYING_WATCH.getDefaultStack();
        setState(stack, state);
        setMode(stack, TimekeeperWatchMode.ITEM_ACCELERATE);
        return stack;
    }

    public static @NotNull TimekeeperWatchState getState(@NotNull ItemStack stack) {
        int ordinal = stack.getOrDefault(ModItems.TIMEKEEPER_WATCH_STATE, TimekeeperWatchState.NORMAL.ordinal());
        TimekeeperWatchState[] values = TimekeeperWatchState.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return TimekeeperWatchState.NORMAL;
        }
        return values[ordinal];
    }

    public static void setState(@NotNull ItemStack stack, @NotNull TimekeeperWatchState state) {
        stack.set(ModItems.TIMEKEEPER_WATCH_STATE, state.ordinal());
    }

    public static @NotNull TimekeeperWatchMode getMode(@NotNull ItemStack stack) {
        int ordinal = stack.getOrDefault(ModItems.TIMEKEEPER_WATCH_MODE, TimekeeperWatchMode.ITEM_ACCELERATE.ordinal());
        TimekeeperWatchMode[] values = TimekeeperWatchMode.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return TimekeeperWatchMode.ITEM_ACCELERATE;
        }
        return values[ordinal];
    }

    public static void setMode(@NotNull ItemStack stack, @NotNull TimekeeperWatchMode mode) {
        stack.set(ModItems.TIMEKEEPER_WATCH_MODE, mode.ordinal());
    }

    public static boolean isUsableWatch(@NotNull ItemStack stack) {
        return stack.isOf(ModItems.DYING_WATCH) && !getState(stack).isBroken();
    }

    @Override
    public Text getName(ItemStack stack) {
        if (getState(stack).isElegant()) {
            return Text.translatable("item.noellesroles.elegant_watch");
        }
        return super.getName(stack);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        TimekeeperWatchState state = getState(stack);
        TimekeeperWatchMode mode = getMode(stack);

        if (state.isBroken()) {
            if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
                TimekeeperAbility.sendActionbar(serverPlayer, Text.translatable("message.noellesroles.timekeeper.watch_broken"));
            }
            return TypedActionResult.fail(stack);
        }

        if (mode == TimekeeperWatchMode.REWIND) {
            if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer && !TimekeeperAbility.canStartRewindCharge(serverPlayer, stack)) {
                return TypedActionResult.fail(stack);
            }
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            TimekeeperAbility.tryUseWatchMode(serverPlayer, stack, mode);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            TimekeeperAbility.tryUseWatchMode(serverPlayer, stack, TimekeeperWatchMode.REWIND);
        }
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return TimekeeperConstants.REWIND_CHARGE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

}
