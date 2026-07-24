package org.agmas.noellesroles.roles.detective;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.AbilityPlayerComponent;

/**
 * 侦探主动能力。
 */
public final class DetectiveAbility {
    private DetectiveAbility() {
    }

    public static void handle(ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRunning()
                || !gameWorld.isRole(player, NoellesRoleRegistry.DETECTIVE)
                || !GameFunctions.isPlayerAliveAndSurvival(player)) {
            return;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(player);
        if (ability.cooldown > 0) {
            return;
        }

        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        if (shop.balance < DetectiveConstants.ABILITY_PRICE) {
            return;
        }

        /*
         * 目标必须由服务端重新射线判定。
         * 客户端准心 HUD 只说明“现在看起来可以调查”，但真正发动时仍以服务端视角为准，
         * 防止客户端伪造目标 id、越距离调查或调查已死亡玩家。
         */
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity target && GameFunctions.isPlayerAliveAndSurvival(target),
                DetectiveConstants.TARGET_RANGE
        );
        if (!(hitResult instanceof EntityHitResult entityHitResult)
                || !(entityHitResult.getEntity() instanceof ServerPlayerEntity targetPlayer)) {
            return;
        }

        Role targetRole = gameWorld.getRole(targetPlayer);
        if (targetRole == null) {
            return;
        }

        /*
         * 和 kinssaba 原逻辑保持一致：只有成功命中有职业的目标时才扣钱并进入冷却。
         * 侦探只知道“是否 innocent”，不会直接获得目标具体职业。
         */
        shop.balance -= DetectiveConstants.ABILITY_PRICE;
        shop.sync();

        boolean innocent = targetRole.isInnocent();
        if (innocent) {
            player.sendMessage(Text.translatable("tip.noellesroles.detective.innocent", targetPlayer.getName().getString()).withColor(WatheRoles.CIVILIAN.color()), true);
            player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else {
            player.sendMessage(Text.translatable("tip.noellesroles.detective.notinnocent", targetPlayer.getName().getString()).withColor(WatheRoles.KILLER.color()), true);
            player.playSoundToPlayer(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        NbtCompound extra = new NbtCompound();
        extra.putBoolean("innocent", innocent);
        extra.putInt("price", DetectiveConstants.ABILITY_PRICE);
        GameRecordManager.recordSkillUse(player, NoellesEventIds.DETECTIVE_CHECK_EVENT, targetPlayer, extra);

        ability.setCooldown(DetectiveConstants.ABILITY_COOLDOWN_TICKS);
    }
}
