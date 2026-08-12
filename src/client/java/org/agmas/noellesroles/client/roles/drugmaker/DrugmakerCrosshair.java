package org.agmas.noellesroles.client.roles.drugmaker;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 毒药师吹箭 / 注射器准心提示。
 *
 * <p>吹箭只需要切换 Wathe 3x3 目标准心；注射器和匕首一样需要在准心下方显示冷却/可攻击图标。
 * 客户端这里只负责提示，服务端仍会重新校验距离、存活状态和冷却。</p>
 */
public final class DrugmakerCrosshair {
    private DrugmakerCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/drugmaker/items"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                DrugmakerCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!context.mainHandStack().isOf(ModItems.BLOWGUN) && !context.mainHandStack().isOf(ModItems.POISON_INJECTOR)) {
            return CrosshairHudApi.Result.PASS;
        }

        PlayerEntity player = context.player();
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        ItemCooldownManager manager = player.getItemCooldownManager();
        if (context.mainHandStack().isOf(ModItems.BLOWGUN)) {
            boolean target = hasTarget(player, DrugmakerConstants.BLOWGUN_TARGET_RANGE)
                    && (ignoresCooldown || !manager.isCoolingDown(context.mainHandStack().getItem()));
            CrosshairHudApi.renderStandardCrosshair(context, target);
        } else {
            boolean target = hasTarget(player, DrugmakerConstants.POISON_INJECTOR_TARGET_RANGE)
                    && (ignoresCooldown || !manager.isCoolingDown(context.mainHandStack().getItem()));
            float progress = ignoresCooldown
                    ? 1.0F
                    : 1.0F - manager.getCooldownProgress(context.mainHandStack().getItem(), context.tickDelta());
            CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target, progress);
        }
        return CrosshairHudApi.Result.HANDLED;
    }

    private static boolean hasTarget(@NotNull PlayerEntity player, float range) {
        HitResult hitResult = ProjectileUtil.getCollision(
                player,
                entity -> entity instanceof PlayerEntity targetPlayer
                        && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                        && TargetVisibilityApi.canTargetPlayer(player, targetPlayer),
                range
        );
        return hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity;
    }
}
