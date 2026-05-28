package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.entities.ThrowingAxeEntity;

/**
 * 飞斧物品。
 * 1. 长按右键蓄力。
 * 2. 松手后生成飞斧实体。
 * 3. 投掷成功后进入与匕首一致的冷却。
 */
public class ThrowingAxeItem extends Item {

    public ThrowingAxeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) {
            return;
        }

        int useDuration = this.getMaxUseTime(stack, user) - remainingUseTicks;
        float power = getPowerForTime(useDuration);

        // 蓄力太短时不发射，避免误触。
        if (power < 0.25F) {
            return;
        }

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        if (!world.isClient) {
            ThrowingAxeEntity axeEntity = new ThrowingAxeEntity(NoellesRolesEntities.THROWING_AXE_ENTITY_TYPE, world);
            axeEntity.setOwner(player);
            axeEntity.initFromStack(stack);
            axeEntity.setPosition(player.getX(), player.getEyeY() - 0.1D, player.getZ());

            // 让飞斧在低蓄力时也能扔出去，高蓄力时飞得更远。
            float velocity = 0.4F + power * 2.0F;
            axeEntity.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, velocity, 1.0F);
            world.spawnEntity(axeEntity);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, null);
            }
        }

        if (!player.isCreative()) {
            stack.decrement(1);
            player.getItemCooldownManager().set(
                    ModItems.THROWING_AXE,
                    GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.THROWING_AXE, 0)
            );
        }

        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    /**
     * 复用弓类物品常见的蓄力曲线，让投掷手感更自然。
     */
    public static float getPowerForTime(int time) {
        float power = (float) time / 20.0F;
        power = (power * power + power * 2.0F) / 3.0F;
        if (power > 1.0F) {
            power = 1.0F;
        }
        return power;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }
}
