package org.agmas.noellesroles.item;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * 毒液注射器。
 */
public class PoisonInjectorItem extends Item {
    public PoisonInjectorItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, @NotNull PlayerEntity player, @NotNull LivingEntity entity, @NotNull Hand hand) {
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        if ((!ignoresCooldown && player.getItemCooldownManager().isCoolingDown(this)) || !(entity instanceof PlayerEntity targetPlayer)) {
            return ActionResult.FAIL;
        }
        if (player.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        /*
         * 只有服务端确认目标是玩家后才写冷却；旁观/创造玩家按需求不受冷却影响。
         * 注射器不是一次性物品，所以不会消耗手持 stack。
         */
        if (!ignoresCooldown) {
            player.getItemCooldownManager().set(this, DrugmakerConstants.POISON_INJECTOR_COOLDOWN_TICKS);
        }

        PlayerPoisonComponent targetPoison = PlayerPoisonComponent.KEY.get(targetPlayer);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        NbtCompound poisonData = player instanceof ServerPlayerEntity serverPlayer
                ? GameFunctions.createReplayItemData(serverPlayer.getServerWorld(), stack)
                : null;

        if (gameWorld.isRole(targetPlayer, NoellesRoleRegistry.ROBOT)) {
            recordRobotFailedUse(player, targetPlayer);
            player.sendMessage(Text.translatable("tip.noellesroles.drugmaker.poison_failed").withColor(Color.RED.getRGB()), true);
            player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            return ActionResult.SUCCESS;
        }

        if (player instanceof ServerPlayerEntity serverPlayer && targetPlayer instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), serverTarget, null);
        }
        if (targetPoison.poisonTicks > 0) {
            /*
             * 目标已经中毒时，注射器会直接触发毒杀，并把当前物品快照交给死亡回放解析。
             */
            GameFunctions.killPlayer(targetPlayer, true, player, GameConstants.DeathReasons.POISON, poisonData);
            player.playSoundToPlayer(SoundEvents.ENTITY_SPIDER_STEP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        } else {
            int poisonTicks = PlayerPoisonComponent.clampTime.getLeft()
                    + player.getRandom().nextInt(PlayerPoisonComponent.clampTime.getRight() - PlayerPoisonComponent.clampTime.getLeft());
            targetPoison.setDetailedPoisonTicks(poisonTicks, player.getUuid(), GameConstants.DeathReasons.POISON, poisonData);
            player.playSoundToPlayer(SoundEvents.ENTITY_SPIDER_DEATH, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        return ActionResult.SUCCESS;
    }

    private void recordRobotFailedUse(@NotNull PlayerEntity player, @NotNull PlayerEntity targetPlayer) {
        if (player instanceof ServerPlayerEntity serverPlayer && targetPlayer instanceof ServerPlayerEntity serverTarget) {
            NbtCompound extra = new NbtCompound();
            extra.putBoolean("robot_failed", true);
            GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), serverTarget, extra);
        }
    }
}
