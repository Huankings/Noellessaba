package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.roles.winder.WindMarkPlayerComponent;
import org.agmas.noellesroles.roles.winder.WinderConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 风之印记。
 *
 * <p>使用方式分两种：
 * 1. 对准玩家右键，立即给目标挂上烙印；
 * 2. 没有对准玩家时，右键蓄力 1 秒，松手后给自己挂上烙印。
 */
public class WindMarkItem extends Item {

    public WindMarkItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerEntity target)) {
            return ActionResult.PASS;
        }

        if (user.getWorld().isClient) {
            // 客户端先确认这次交互已经命中了玩家，避免继续落到“自我蓄力”分支。
            return ActionResult.SUCCESS;
        }

        if (!canApplyMarkNow(user) || !canApplyMarkNow(target)) {
            return ActionResult.CONSUME;
        }

        if (tryApplyMark(user, target)) {
            stack.decrementUnlessCreative(1, user);
            user.incrementStat(Stats.USED.getOrCreateStat(this));
        }

        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, @NotNull PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player) || world.isClient) {
            return;
        }

        int usedTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
        if (usedTicks < WinderConstants.MARK_SELF_CHARGE_TICKS) {
            return;
        }

        if (!canApplyMarkNow(player)) {
            return;
        }

        if (tryApplyMark(player, player)) {
            stack.decrementUnlessCreative(1, player);
            player.incrementStat(Stats.USED.getOrCreateStat(this));
        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    /**
     * 只有局内仍然存活的玩家，才允许被真正挂上风之印记。
     * 这样能直接和后面的透视、保命条件保持一致。
     */
    private static boolean canApplyMarkNow(@NotNull PlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        return gameWorld.isRunning() && GameFunctions.isPlayerAliveAndSurvival(player);
    }

    /**
     * 统一处理成功烙印与“烙印仍存在”的提示。
     *
     * @return 本次是否真的成功挂上了新烙印
     */
    private static boolean tryApplyMark(@NotNull PlayerEntity user, @NotNull PlayerEntity target) {
        WindMarkPlayerComponent component = WindMarkPlayerComponent.KEY.get(target);
        if (component.hasActiveMark()) {
            user.sendMessage(Text.translatable("message.noellesroles.wind_mark.exists", target.getDisplayName()), true);
            return false;
        }

        component.applyMark(user);
        if (user instanceof net.minecraft.server.network.ServerPlayerEntity serverUser) {
            NbtCompound extra = new NbtCompound();
            extra.putUuid("target_player", target.getUuid());
            GameRecordManager.recordGlobalEvent(serverUser.getServerWorld(), org.agmas.noellesroles.Noellesroles.WINDER_WIND_MARK_APPLIED_EVENT, serverUser, extra);
        }
        user.sendMessage(Text.translatable("message.noellesroles.wind_mark.applied", target.getDisplayName()), true);
        return true;
    }
}
