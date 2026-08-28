package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.NoellesRolesEntities;
import org.agmas.noellesroles.registry.NoellesEventIds;

/**
 * 魔法屏障物品。
 *
 * <p>右键蓄力 0.5 秒后生成一个前飞、逐渐扩大的球体粒子实体。
 * 正式玩家使用后消耗物品并进入 10 秒物品冷却；旁观/创造调试玩家不消耗也不吃冷却。</p>
 */
public class LichMagicBarrierItem extends Item {
    public LichMagicBarrierItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!LichItemUseRules.canUseLichDebugAwareItem(user, this)) {
            return TypedActionResult.fail(stack);
        }

        /*
         * 屏障和简易法杖一样存在蓄力窗口。
         * 起手状态交给 LichItemUseRules 保存，防止正常玩家蓄力中死亡后松手仍完成释放。
         */
        LichItemUseRules.beginChargedUse(user, stack);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) {
            return;
        }
        if (!LichItemUseRules.finishChargedUse(player, this, stack)) {
            return;
        }

        int useDuration = this.getMaxUseTime(stack, user) - remainingUseTicks;
        if (useDuration < LichConstants.MAGIC_BARRIER_MIN_CHARGE_TICKS) {
            return;
        }

        LichItemUseRules.playUseSoundFromPlayer(
                world,
                player,
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                LichConstants.MAGIC_BARRIER_CAST_SOUND_VOLUME,
                LichConstants.MAGIC_BARRIER_CAST_SOUND_PITCH
        );

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            LichMagicBarrierEntity entity = new LichMagicBarrierEntity(NoellesRolesEntities.LICH_MAGIC_BARRIER_ENTITY_TYPE, world);
            entity.setOwner(serverPlayer);
            entity.setPosition(serverPlayer.getX(), serverPlayer.getEyeY() + LichConstants.MAGIC_BARRIER_SPAWN_EYE_Y_OFFSET, serverPlayer.getZ());
            entity.setVelocity(serverPlayer.getRotationVector().normalize().multiply(LichConstants.MAGIC_BARRIER_SPEED_BLOCKS_PER_TICK));
            if (world.spawnEntity(entity)) {
                /*
                 * 只有服务端真正生成出屏障实体后才写入释放回放。
                 * 这样如果后续因为世界规则或其他模组阻止生成，不会留下“释放成功”的误导记录。
                 */
                GameRecordManager.recordGlobalEvent(
                        serverPlayer.getServerWorld(),
                        NoellesEventIds.LICH_MAGIC_BARRIER_CAST_EVENT,
                        serverPlayer,
                        null
                );
            }
        }

        if (!GameFunctions.isPlayerSpectatingOrCreative(player)) {
            stack.decrement(1);
            player.getItemCooldownManager().set(this, LichConstants.MAGIC_BARRIER_ITEM_COOLDOWN_TICKS);
        }
        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return LichConstants.MAGIC_BARRIER_MAX_USE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }
}
