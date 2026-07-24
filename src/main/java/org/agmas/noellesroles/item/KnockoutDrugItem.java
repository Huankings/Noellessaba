package org.agmas.noellesroles.item;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.cca.GameWorldComponent;
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
import org.agmas.noellesroles.roles.kidnapper.KidnapperComponent;
import org.agmas.noellesroles.roles.kidnapper.KidnapperConstants;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * 绑匪迷药。
 */
public class KnockoutDrugItem extends Item {
    public KnockoutDrugItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, @NotNull PlayerEntity player, @NotNull LivingEntity entity, @NotNull Hand hand) {
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        if ((!ignoresCooldown && player.getItemCooldownManager().isCoolingDown(this))
                || player.isSneaking()
                || !(entity instanceof PlayerEntity targetPlayer)) {
            return ActionResult.FAIL;
        }
        if (player.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        /*
         * kinssaba 原逻辑是在确认点到玩家后立刻结算迷药消耗。
         * 因此即使目标是机器人免疫，正式对局里的使用者仍会消耗物品并进入冷却；
         * 旁观/创造玩家则按需求完全跳过冷却和消耗，方便连续测试。
         */
        if (!ignoresCooldown) {
            player.getItemCooldownManager().set(this, KidnapperConstants.KNOCKOUT_DRUG_COOLDOWN_TICKS);
            player.getStackInHand(hand).decrement(1);
        }

        KidnapperComponent targetControlled = KidnapperComponent.KEY.get(targetPlayer);
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld.isRole(targetPlayer, NoellesRoleRegistry.ROBOT)) {
            recordRobotFailedUse(player, targetPlayer);
            player.sendMessage(Text.translatable("tip.noellesroles.kidnapper.daze_failed").withColor(Color.RED.getRGB()), true);
            player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            return ActionResult.SUCCESS;
        }

        if (targetControlled.controlTicks <= 0) {
            targetControlled.startControl(player);
            if (player instanceof ServerPlayerEntity serverPlayer && targetPlayer instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), serverTarget, null);
            }
            player.playSoundToPlayer(SoundEvents.ENTITY_SHEEP_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            targetPlayer.playSoundToPlayer(SoundEvents.ENTITY_SHEEP_AMBIENT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private void recordRobotFailedUse(@NotNull PlayerEntity player, @NotNull PlayerEntity targetPlayer) {
        if (player instanceof ServerPlayerEntity serverPlayer && targetPlayer instanceof ServerPlayerEntity serverTarget) {
            NbtCompound extra = new NbtCompound();
            extra.putBoolean("robot_failed", true);
            GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), serverTarget, extra);
        }
    }
}
