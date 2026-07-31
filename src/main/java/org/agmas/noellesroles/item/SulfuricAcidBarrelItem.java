package org.agmas.noellesroles.item;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.agmas.noellesroles.roles.cleaner.CleanerConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 硫酸桶：对尸体使用后溶解尸体。
 */
public class SulfuricAcidBarrelItem extends Item {

    public SulfuricAcidBarrelItem(@NotNull Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, @NotNull PlayerEntity player, @NotNull LivingEntity entity, Hand hand) {
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.FAIL;
        }

        if (!player.getWorld().isClient && entity instanceof PlayerBodyEntity playerBody) {
            if (!TargetVisibilityApi.canInteractWithBody(player, playerBody)) {
                return ActionResult.PASS;
            }

            if (player instanceof ServerPlayerEntity serverPlayer) {
                /*
                 * 回放里保存尸体归属者 UUID，而不是保存显示名。
                 * 这样即使玩家离线，回放系统也能从本局 player info 缓存里还原名字。
                 */
                NbtCompound extra = new NbtCompound();
                extra.putUuid("body_owner", playerBody.getPlayerUuid());
                GameRecordManager.recordItemUse(serverPlayer, NoellesEventIds.SULFURIC_ACID_BARREL_USE_EVENT, null, extra);
            }

            /*
             * kinssaba 的硫酸桶调用 setItemAfterUsing(player, item, null)，因此只写冷却、不扣物品数量。
             * 这里显式保留这个语义：硫酸桶是可重复使用道具，而不是一次性消耗品。
             */
            if (GameFunctions.isPlayerAliveAndSurvival(player)) {
                player.getItemCooldownManager().set(this, CleanerConstants.SULFURIC_ACID_BARREL_COOLDOWN_TICKS);
            }

            playerBody.discard();
            player.getWorld().playSound(
                    null,
                    playerBody.getX(),
                    playerBody.getY() + 0.1F,
                    playerBody.getZ(),
                    SoundEvents.ITEM_BUCKET_EMPTY_LAVA,
                    SoundCategory.PLAYERS,
                    1.0F,
                    0.5F
            );

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            if (gameWorld.isRole(player, NoellesRoleRegistry.CLEANER)) {
                PlayerShopComponent.KEY.get(player).addToBalance(CleanerConstants.DISSOLVE_REWARD_COINS);
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
