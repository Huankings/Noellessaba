package org.agmas.noellesroles.item;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.roles.dreamer.DreamerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 梦之印记。
 *
 * <p>成功对玩家使用后，会在目标身上写入一层梦之印记护盾。
 * 物品只在服务端真正标记成功时消耗并记录回放，避免客户端空挥或重复标记产生假事件。</p>
 */
public class DreamImprintItem extends Item {
    public DreamImprintItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, @NotNull PlayerEntity player, @NotNull LivingEntity entity, @NotNull Hand hand) {
        if (!(entity instanceof PlayerEntity targetPlayer)) {
            return ActionResult.PASS;
        }

        if (player.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(player, NoellesRoleRegistry.DREAMER)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
            return ActionResult.CONSUME;
        }

        DreamerComponent targetDream = DreamerComponent.KEY.get(targetPlayer);
        if (targetDream.dreamArmor > 0) {
            return ActionResult.CONSUME;
        }

        stack.decrementUnlessCreative(1, player);
        player.incrementStat(Stats.USED.getOrCreateStat(this));
        targetDream.imprintDreamer(player);

        if (player instanceof ServerPlayerEntity serverPlayer && targetPlayer instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemUse(serverPlayer, NoellesEventIds.DREAM_IMPRINT_SHIELD_SOURCE, serverTarget, null);
        }

        player.sendMessage(Text.translatable("tip.noellesroles.dreamer.imprint", targetPlayer.getName().getString()).withColor(NoellesRoleRegistry.DREAMER.color()), true);
        player.playSoundToPlayer(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        targetPlayer.sendMessage(Text.translatable("tip.noellesroles.dreamer.imprint", player.getName().getString()).withColor(NoellesRoleRegistry.DREAMER.color()), true);
        targetPlayer.playSoundToPlayer(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return ActionResult.SUCCESS;
    }
}
