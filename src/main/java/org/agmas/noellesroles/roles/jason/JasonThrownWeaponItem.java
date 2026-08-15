package org.agmas.noellesroles.roles.jason;

import dev.doctor4t.wathe.game.GameConstants;
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

import java.util.UUID;

/**
 * 杰森蓄力投掷物。
 *
 * <p>普通投掷武器、杰森模式飞镐和投掷油桶共用这个物品类：
 * 它们的共同点是“右键蓄力，松手生成自定义实体”，差异由物品 id 和 {@link JasonConstants} 决定。</p>
 */
public class JasonThrownWeaponItem extends Item {
    public JasonThrownWeaponItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!GameFunctions.isPlayerAliveAndSurvival(user)) {
            /*
             * 只限制 Wathe 定义的局内存活玩家。
             * 非存活旁观/创造玩家不应该被杰森物品机制额外卡住交互或观战体验。
             */
            return TypedActionResult.pass(stack);
        }
        if (JasonWoundManager.isWoundedActionLocked(user)) {
            return TypedActionResult.fail(stack);
        }
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) {
            return;
        }
        if (!GameFunctions.isPlayerAliveAndSurvival(player) || JasonWoundManager.isWoundedActionLocked(player)) {
            return;
        }

        int useDuration = this.getMaxUseTime(stack, user) - remainingUseTicks;
        float power = getPowerForTime(useDuration);
        if (useDuration < JasonConstants.THROW_MIN_CHARGE_TICKS || power <= 0.0F) {
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
                0.9F + world.random.nextFloat() * 0.15F
        );

        if (!world.isClient) {
            JasonThrownWeaponEntity entity = new JasonThrownWeaponEntity(NoellesRolesEntities.JASON_THROWN_WEAPON_ENTITY_TYPE, world);
            entity.setOwner(player);
            entity.initFromStack(stack);
            entity.setPosition(player.getX(), player.getEyeY() - 0.1D, player.getZ());
            float velocity = resolveVelocity(stack, power);
            entity.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, velocity, JasonConstants.PROJECTILE_DIVERGENCE);
            world.spawnEntity(entity);

            if (player instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordItemUse(
                        serverPlayer,
                        Registries.ITEM.getId(this),
                        null,
                        GameFunctions.createReplayItemData(serverPlayer.getServerWorld(), stack)
                );
                if (stack.isOf(ModItems.THROWING_JERRY_CAN)) {
                    giveOnceLighterForCan(serverPlayer, entity.getUuid());
                }
            }
        }

        if (!player.isCreative() && !stack.isOf(ModItems.THROWING_PICKAXE)) {
            stack.decrement(1);
            player.getItemCooldownManager().set(this, GameConstants.ITEM_COOLDOWNS.getOrDefault(this, 0));
        } else if (stack.isOf(ModItems.THROWING_PICKAXE)) {
            // 杰森模式飞镐按需求投掷不消耗、无冷却；这里显式清掉可能存在的旧冷却兜底。
            player.getItemCooldownManager().set(this, 0);
        }

        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    private static void giveOnceLighterForCan(ServerPlayerEntity player, UUID canUuid) {
        /*
         * 每个投掷油桶都要生成一枚独立打火机。
         * 打火机的自定义数据直接绑定油桶实体 UUID，后续玩家可以通过选择不同格子的打火机
         * 来点燃对应的落地油桶，自动燃烧时也能精准清理这枚打火机。
         */
        player.giveItemStack(JasonOnceLighterItem.createBoundStack(canUuid, player.getUuid()));
    }

    private static float resolveVelocity(ItemStack stack, float power) {
        if (stack.isOf(ModItems.THROWING_JERRY_CAN)) {
            return JasonConstants.JERRY_CAN_VELOCITY_BASE + power * JasonConstants.JERRY_CAN_VELOCITY_BONUS;
        }
        return JasonConstants.THROW_VELOCITY_BASE + power * JasonConstants.THROW_VELOCITY_BONUS;
    }

    /**
     * 杰森投掷物使用线性蓄力：达到 0.7 秒即可满功率。
     * 这样它和旧飞斧的长蓄力曲线区分开，手感更贴近用户要求的“蓄力更短、更强”。
     */
    public static float getPowerForTime(int time) {
        return Math.min(1.0F, Math.max(0.0F, time / (float) JasonConstants.THROW_MIN_CHARGE_TICKS));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return JasonConstants.THROW_MAX_USE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }
}
