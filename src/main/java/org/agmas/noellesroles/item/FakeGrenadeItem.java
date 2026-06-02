package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.cca.PlayerGrenadeComponent;
import dev.doctor4t.wathe.entity.GrenadeEntity;
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
import org.jetbrains.annotations.NotNull;

/**
 * 假手雷。
 *
 * <p>实现思路尽量贴近 wathe 原版手雷：
 * 1. 保留按住右键蓄力投掷的交互。
 * 2. 保留原版手雷的出手音效、抛射速度曲线和实体类型。
 * 3. 额外把“这是一枚假手雷”的信息写进投掷物的 ItemStack 中，
 *    供后续的 GrenadeEntity Mixin 在爆炸时识别并取消击杀逻辑。
 *
 * <p>这样做的好处是：
 * 原版客户端渲染、飞行轨迹、命中时机和大部分粒子音效都能继续复用，
 * 我们只需要在真正发生爆炸结算时，精准移除致死效果即可。
 */
public class FakeGrenadeItem extends GrenadeItem {

    /**
     * 与 wathe 原版手雷保持一致：最多蓄力 20 tick，也就是 1 秒。
     */
    private static final int MAX_CHARGE_TIME = 20;

    /**
     * 不蓄力时的基础投掷速度，直接与原版手雷保持一致。
     */
    private static final float BASE_THROW_SPEED = 0.5F;

    /**
     * 满蓄力时的最大投掷速度，直接与原版手雷保持一致。
     */
    private static final float MAX_THROW_SPEED = 1.3F;

    public FakeGrenadeItem(Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        /*
         * 这里不能继续直接沿用父类 GrenadeItem.use()。
         * 因为 Wathe 现在的父类已经会在“直投模式”下立刻生成真手雷逻辑的投掷物，
         * 那样 fake_grenade 自己的 ItemStack 标记就会丢失，后续 mixin 也就无法识别这是一枚假手雷。
         *
         * 所以我们和 silent_grenade 一样，在 fake_grenade 自己这里完整接管两种投掷模式：
         * 1. 直投模式：立即按基础速度投出；
         * 2. 蓄力模式：进入持续使用状态，等松开右键后再按蓄力速度投出。
         */
        if (PlayerGrenadeComponent.KEY.get(user).isDirectThrowMode()) {
            if (!world.isClient) {
                this.noellesroles$throwFakeGrenade(world, user, stack, BASE_THROW_SPEED);
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

        // 这里完全沿用真手雷的蓄力换算，让假手雷的出手手感与真手雷保持一致。
        float throwSpeed = this.noellesroles$getThrowSpeed(stack, user, remainingUseTicks);
        this.noellesroles$throwFakeGrenade(world, player, stack, throwSpeed);
    }

    /**
     * 无论当前是直投还是蓄力，最终都统一走这里生成投掷物。
     *
     * <p>这样可以确保：
     * 1. 飞出去的一定是携带 fake_grenade 自身 ItemStack 的 GrenadeEntity；
     * 2. FakeGrenadeMixin 在碰撞时一定能识别出“这是假的”，从而只播表现、不结算真实伤害。</p>
     */
    private void noellesroles$throwFakeGrenade(@NotNull World world, @NotNull PlayerEntity player, @NotNull ItemStack stack, float throwSpeed) {
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

        // 关键标记：
        // 让飞出去的投掷物携带 fake_grenade 自身的 ItemStack。
        // 后面的 Mixin 会通过这里的物品类型判断“只放效果、不杀人”。
        ItemStack thrownStack = stack.copy();
        thrownStack.setCount(1);
        grenade.setItem(thrownStack);

        grenade.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, throwSpeed, 1.0F);
        world.spawnEntity(grenade);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // 假手雷的回放语义是“投掷了一枚假手雷”，因此在实体真正生成成功后再记录。
            GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, null);
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

    /**
     * 计算本次真正参与投掷速度换算的蓄力时长。
     * 超过 1 秒的部分会被截断，避免一直按住后速度继续上升。
     */
    private int noellesroles$getChargeTicks(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        int usedTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
        return MathHelper.clamp(usedTicks, 0, MAX_CHARGE_TIME);
    }

    /**
     * 把蓄力进度线性映射到投掷初速度。
     * 这里完全照抄原版手雷公式，确保假手雷的抛物线表现一致。
     */
    private float noellesroles$getThrowSpeed(ItemStack stack, LivingEntity user, int remainingUseTicks) {
        float chargeProgress = (float) this.noellesroles$getChargeTicks(stack, user, remainingUseTicks) / MAX_CHARGE_TIME;
        return BASE_THROW_SPEED + (MAX_THROW_SPEED - BASE_THROW_SPEED) * chargeProgress;
    }
}
