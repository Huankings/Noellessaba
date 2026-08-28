package org.agmas.noellesroles.roles.lich;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesEntities;

/**
 * 巫妖“简易法杖”。
 *
 * <p>右键蓄力 0.6 秒后释放，向视角前方扇形发射 5 个法术骷髅。
 * 这件物品购买后一次性使用；旁观/创造调试玩家按用户要求不受冷却和消耗限制。</p>
 */
public class LichStaffItem extends Item {
    public LichStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!LichItemUseRules.canUseLichDebugAwareItem(user, this)) {
            return TypedActionResult.fail(stack);
        }

        /*
         * 客户端和服务端都调用 setCurrentHand：
         * 客户端负责显示拉弓式使用动画与准星进度，服务端负责最终 onStoppedUsing 校验。
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
        if (useDuration < LichConstants.ONCE_STAFF_MIN_CHARGE_TICKS) {
            return;
        }

        LichItemUseRules.playUseSoundFromPlayer(
                world,
                player,
                SoundEvents.ENTITY_WITHER_SHOOT,
                LichConstants.SKELETON_SHOOT_SOUND_VOLUME,
                LichConstants.SKELETON_SHOOT_SOUND_PITCH
        );

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            LichSkeletonSkullEntity.spawnFan(
                    serverPlayer,
                    NoellesRolesEntities.LICH_SKELETON_SKULL_ENTITY_TYPE,
                    LichSkeletonKind.SPELL,
                    LichConstants.ONCE_STAFF_SKELETON_COUNT,
                    LichConstants.ONCE_STAFF_FAN_DEGREES,
                    LichConstants.ONCE_STAFF_RANGE_BLOCKS,
                    LichConstants.ONCE_STAFF_SKULL_SPEED_BLOCKS_PER_TICK
            );
            GameRecordManager.recordItemUse(
                    serverPlayer,
                    Registries.ITEM.getId(ModItems.ONCE_STAFF),
                    null,
                    LichSkeletonKind.SPELL.createReplayData(serverPlayer.getServerWorld(), stack)
            );
        }

        if (!GameFunctions.isPlayerSpectatingOrCreative(player)) {
            stack.decrement(1);
            player.getItemCooldownManager().set(this, LichConstants.ONCE_STAFF_COOLDOWN_TICKS);
        }
        player.incrementStat(Stats.USED.getOrCreateStat(this));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return LichConstants.ONCE_STAFF_MAX_USE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }
}
