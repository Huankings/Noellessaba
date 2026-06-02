package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerGrenadeComponent;
import dev.doctor4t.wathe.entity.GrenadeEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.item.GrenadeItem;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.jetbrains.annotations.NotNull;

/**
 * 无声手雷。
 *
 * <p>它保留 Wathe 当前版本手雷的两种投掷模式：</p>
 * <p>1. 直投模式：右键立即按基础速度扔出；</p>
 * <p>2. 蓄力模式：按住右键，最多蓄力 1 秒后再投掷。</p>
 *
 * <p>与原版的区别在于：</p>
 * <p>1. 投出时把自己的 ItemStack 写进 GrenadeEntity，供爆炸时识别；</p>
 * <p>2. 投掷后立刻进入 5 分钟冷却，阻止再次购买；</p>
 * <p>3. 落地爆炸时由 mixin 取消爆炸声，仅保留粒子与伤害判定。</p>
 */
public class SilentGrenadeItem extends GrenadeItem {

    private static final int MAX_CHARGE_TIME = 20;
    private static final float BASE_THROW_SPEED = 0.5F;
    private static final float MAX_THROW_SPEED = 1.3F;

    public SilentGrenadeItem(Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (PlayerGrenadeComponent.KEY.get(user).isDirectThrowMode()) {
            if (!world.isClient) {
                this.throwSilentGrenade(world, user, stack, BASE_THROW_SPEED);
            }
            return TypedActionResult.consume(stack);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user.isSpectator() || !(user instanceof PlayerEntity player) || world.isClient) {
            return;
        }

        float throwSpeed = this.getThrowSpeed(stack, user, remainingUseTicks);
        this.throwSilentGrenade(world, player, stack, throwSpeed);
    }

    private void throwSilentGrenade(@NotNull World world, @NotNull PlayerEntity player, @NotNull ItemStack stack, float throwSpeed) {
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                WatheSounds.ITEM_GRENADE_THROW,
                SoundCategory.NEUTRAL,
                0.5F,
                1F + (world.random.nextFloat() - .5f) / 10f
        );

        GrenadeEntity grenade = new GrenadeEntity(WatheEntities.GRENADE, world);
        grenade.setOwner(player);
        grenade.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());

        /*
         * 关键标记：让飞出去的投掷物随身携带“无声手雷”的真实 ItemStack，
         * 后续爆炸 mixin 才能据此切到无声分支，并把真实物品名写进回放。
         */
        ItemStack thrownStack = stack.copy();
        thrownStack.setCount(1);
        grenade.setItem(thrownStack);

        grenade.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, throwSpeed, 1.0F);
        world.spawnEntity(grenade);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, null);
        }

        if (!player.isCreative()) {
            player.getItemCooldownManager().set(
                    ModItems.SILENT_GRENADE,
                    GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.SILENT_GRENADE, 0)
            );
        }

        player.incrementStat(Stats.USED.getOrCreateStat(this));
        stack.decrementUnlessCreative(1, player);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    private int getChargeTicks(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        int usedTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
        return MathHelper.clamp(usedTicks, 0, MAX_CHARGE_TIME);
    }

    private float getThrowSpeed(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        float chargeProgress = (float) this.getChargeTicks(stack, user, remainingUseTicks) / MAX_CHARGE_TIME;
        return BASE_THROW_SPEED + (MAX_THROW_SPEED - BASE_THROW_SPEED) * chargeProgress;
    }
}
