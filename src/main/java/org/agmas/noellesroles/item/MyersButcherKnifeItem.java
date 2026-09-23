package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.myers.MyersPlayerComponent;
import org.jetbrains.annotations.NotNull;

/** 迈尔斯屠刀：只负责进入/结束使用状态，冲刺结算由玩家组件权威处理。 */
public final class MyersButcherKnifeItem extends Item {
    public MyersButcherKnifeItem(Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, @NotNull Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        boolean debug = GameFunctions.isPlayerSpectatingOrCreative(user);
        if (!debug && user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        MyersPlayerComponent.KEY.get(user).beginCharge();
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, @NotNull LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity player && !world.isClient) {
            MyersPlayerComponent.KEY.get(player).releaseCharge();
        }
    }

    @Override
    public UseAction getUseAction(@NotNull ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public int getMaxUseTime(@NotNull ItemStack stack, @NotNull LivingEntity user) {
        return Integer.MAX_VALUE;
    }
}
