package org.agmas.noellesroles.item;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
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
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.agmas.noellesroles.roles.physician.PhysicianConstants;
import org.agmas.noellesroles.roles.physician.PhysicianTaskmasterCompat;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * 医疗箱。
 *
 * <p>成功对玩家使用时，会同时清除 Wathe 真毒与 Noelles 幻觉试剂。
 * 医师使用成功会得到治疗奖励；物品本身不消耗，只进入冷却。</p>
 */
public class MedicalKitItem extends Item {
    public MedicalKitItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, @NotNull PlayerEntity player, @NotNull LivingEntity entity, @NotNull Hand hand) {
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        if ((!ignoresCooldown && player.getItemCooldownManager().isCoolingDown(this)) || !(entity instanceof PlayerEntity targetPlayer)) {
            return ActionResult.PASS;
        }
        /*
         * 医疗箱虽然是友方交互，但仍会产生客户端成功反馈和服务端治疗回放。
         * 尸体伪装玩家应当像普通尸体一样不可被当作活玩家治疗目标。
         */
        if (!TargetVisibilityApi.canInteractWithPlayer(player, targetPlayer)) {
            return ActionResult.PASS;
        }

        if (player.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        PlayerPoisonComponent targetPoison = PlayerPoisonComponent.KEY.get(targetPlayer);
        DelusionPlayerComponent targetDelusion = DelusionPlayerComponent.KEY.get(targetPlayer);
        boolean hasPoison = targetPoison.poisonTicks > 0;
        boolean hasDelusion = targetDelusion.isActive();
        if (!hasPoison && !hasDelusion) {
            return ActionResult.PASS;
        }

        /*
         * 只在服务端确认目标确实有异常状态后才结算冷却和回放。
         * 这样空点、点错目标、客户端预测都不会产生假治疗记录。
         */
        /*
         * 旁观/创造常用于调试职业机制，按需求不让这类玩家被医疗箱冷却限制。
         * 正常对局里的存活玩家仍然完整走冷却，避免影响正式平衡。
         */
        if (!ignoresCooldown) {
            player.getItemCooldownManager().set(this, PhysicianConstants.MEDICAL_KIT_COOLDOWN_TICKS);
        }
        player.incrementStat(Stats.USED.getOrCreateStat(this));
        if (hasPoison) {
            targetPoison.reset();
        }
        if (hasDelusion) {
            targetDelusion.reset();
        }

        targetPlayer.sendMessage(Text.translatable("tip.noellesroles.physician.medical_kit").withColor(Color.GREEN.getRGB()), true);
        targetPlayer.getWorld().playSound(null, targetPlayer.getBlockPos(), SoundEvents.ENTITY_HORSE_ARMOR, SoundCategory.PLAYERS, 1.0F, 1.0F);

        if (player instanceof ServerPlayerEntity serverPlayer && targetPlayer instanceof ServerPlayerEntity serverTarget) {
            GameRecordManager.recordItemUse(serverPlayer, NoellesEventIds.MEDICAL_KIT_USE_EVENT, serverTarget, null);
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (gameWorld.isRole(player, NoellesRoleRegistry.PHYSICIAN)) {
            PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
            int reward = PhysicianTaskmasterCompat.hasTaskmaster(player)
                    ? PhysicianConstants.MEDICAL_KIT_TASKMASTER_REWARD
                    : PhysicianConstants.MEDICAL_KIT_REWARD;
            /*
             * 这里使用 Wathe 的余额组件入口发放奖励，确保服务端数值与客户端金币 HUD 同步。
             */
            shop.addToBalance(reward);
        }

        return ActionResult.SUCCESS;
    }
}
