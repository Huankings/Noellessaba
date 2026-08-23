package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameFunctions;
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
import org.agmas.noellesroles.entities.ThrowingPanEntity;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.agmas.noellesroles.roles.cook.CookPsychoHandler;

/**
 * 飞锅和疯魔飞锅的蓄力投掷物品。
 *
 * <p>普通飞锅投掷后消耗；疯魔飞锅只允许厨师疯魔期间使用，投掷后不消耗。
 * 旁观/创造调试玩家可以绕过“必须处于厨师疯魔”的限制，方便连续测试命中和回放。</p>
 */
public class ThrowingPanItem extends Item {
    public ThrowingPanItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!canStartUsing(user, stack)) {
            return TypedActionResult.fail(stack);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player) || !canStartUsing(player, stack)) {
            return;
        }

        int useDuration = this.getMaxUseTime(stack, user) - remainingUseTicks;
        float power = getPowerForTime(useDuration);

        // 蓄力太短时不发射，避免轻点右键误扔出关键武器。
        if (power < CookConstants.THROWING_PAN_MIN_POWER) {
            return;
        }

        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_TRIDENT_THROW,
                SoundCategory.PLAYERS,
                CookConstants.THROWING_PAN_THROW_SOUND_VOLUME,
                CookConstants.THROWING_PAN_THROW_SOUND_PITCH
        );

        if (!world.isClient) {
            ThrowingPanEntity panEntity = new ThrowingPanEntity(NoellesRolesEntities.THROWING_PAN_ENTITY_TYPE, world);
            panEntity.setOwner(player);
            panEntity.initFromStack(stack);
            panEntity.setPosition(player.getX(), player.getEyeY() - CookConstants.THROWING_PAN_SPAWN_EYE_Y_OFFSET, player.getZ());

            float velocity = CookConstants.THROWING_PAN_BASE_VELOCITY
                    + power * CookConstants.THROWING_PAN_POWER_VELOCITY_MULTIPLIER;
            panEntity.setVelocity(
                    player,
                    player.getPitch(),
                    player.getYaw(),
                    0.0F,
                    velocity,
                    CookConstants.THROWING_PAN_VELOCITY_DIVERGENCE
            );
            world.spawnEntity(panEntity);

            if (player instanceof ServerPlayerEntity serverPlayer) {
                /*
                 * 两种飞锅的回放都按“通用飞锅名”展示。
                 * 这里 item id 仍记录真实使用的物品，formatter 会强制把显示名替换成 [飞锅]。
                 */
                GameRecordManager.recordItemUse(
                        serverPlayer,
                        Registries.ITEM.getId(this),
                        null,
                        GameFunctions.createReplayItemData(serverPlayer.getServerWorld(), ModItems.THROWING_PAN.getDefaultStack())
                );
            }
        }

        if (!isPsychoThrowingPan(stack) && !player.isCreative()) {
            stack.decrement(1);
            player.getItemCooldownManager().set(this, dev.doctor4t.wathe.game.GameConstants.ITEM_COOLDOWNS.getOrDefault(this, 0));
        } else if (isPsychoThrowingPan(stack)) {
            // 疯魔飞锅按需求投掷不消耗；这里顺手清掉可能存在的旧冷却，保证手感始终连续。
            player.getItemCooldownManager().set(this, 0);
        }

        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    public static float getPowerForTime(int time) {
        float power = (float) time / CookConstants.THROWING_PAN_CHARGE_UNIT_TICKS;
        power = (power * power + power * CookConstants.THROWING_PAN_POWER_LINEAR_MULTIPLIER)
                / CookConstants.THROWING_PAN_POWER_DIVISOR;
        if (power > CookConstants.THROWING_PAN_MAX_POWER) {
            power = CookConstants.THROWING_PAN_MAX_POWER;
        }
        return power;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return CookConstants.THROWING_PAN_MAX_USE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    private static boolean canStartUsing(PlayerEntity player, ItemStack stack) {
        boolean debugPlayer = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!debugPlayer && player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
            return false;
        }

        /*
         * 疯魔飞锅的正式限制是“厨师疯魔期间才能使用”。
         * 调试玩家可以绕过该限制，方便在旁观/创造状态下直接测试命中存活玩家。
         */
        return !isPsychoThrowingPan(stack)
                || debugPlayer
                || CookPsychoHandler.isCookPsychoActive(player);
    }

    private static boolean isPsychoThrowingPan(ItemStack stack) {
        return stack.isOf(ModItems.PSYCHO_THROWING_PAN);
    }
}
