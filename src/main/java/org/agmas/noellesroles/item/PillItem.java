package org.agmas.noellesroles.item;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.physician.PhysicianConstants;
import org.agmas.noellesroles.roles.physician.PhysicianPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 药丸。
 *
 * <p>食用后给自己一层一次性护盾。真正挡死时是否生效由死亡链决定，
 * 其中 Noelles 强制死亡规则会先执行，所以药丸不会挡落轨、炸弹、镇静过量、狙击枪等不可挡死因。</p>
 */
public class PillItem extends Item {
    public PillItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        if (!ignoresCooldown && player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            /*
             * 药丸调试时经常需要反复吃、反复触发死亡链。
             * 旁观/创造玩家只保留效果结算，不再写入物品冷却。
             */
            if (!ignoresCooldown) {
                player.getItemCooldownManager().set(this, PhysicianConstants.PILL_COOLDOWN_TICKS);
            }
            player.incrementStat(Stats.USED.getOrCreateStat(this));
            PhysicianPlayerComponent.KEY.get(player).givePillArmor();

            if (player instanceof ServerPlayerEntity serverPlayer) {
                GameRecordManager.recordItemUse(serverPlayer, Noellesroles.PILL_SHIELD_SOURCE, null, null);
            }
            player.playSoundToPlayer(SoundEvents.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

        return TypedActionResult.success(stack, false);
    }
}
