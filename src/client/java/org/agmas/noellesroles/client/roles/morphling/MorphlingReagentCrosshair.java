package org.agmas.noellesroles.client.roles.morphling;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.morphling.MorphlingConstants;
import org.agmas.noellesroles.roles.morphling.MorphlingReagentService;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * 变形试剂准心提示。
 *
 * <p>未采样时允许瞄准活玩家或尸体；已有样本后只允许瞄准活玩家标记。
 * 如果目标正好就是样本 UUID，准心保持普通形态，对应服务端“不消耗试剂”的失败规则。</p>
 */
public final class MorphlingReagentCrosshair {
    private MorphlingReagentCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/morphling/reagent"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                MorphlingReagentCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        ClientPlayerEntity player = context.player();
        if (!context.mainHandStack().isOf(ModItems.MORPH_REAGENT)
                || !GameFunctions.isPlayerAliveAndSurvival(player)
                || !GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.MORPHLING)) {
            return CrosshairHudApi.Result.PASS;
        }

        boolean hasSample = MorphlingReagentService.hasSample(context.mainHandStack());
        Optional<UUID> sampleUuid = MorphlingReagentService.sampleUuid(context.mainHandStack());
        boolean target = ProjectileUtil.getCollision(
                player,
                entity -> {
                    if (entity instanceof PlayerEntity targetPlayer) {
                        if (targetPlayer == player
                                || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)
                                || !TargetVisibilityApi.canTargetPlayer(player, targetPlayer)) {
                            return false;
                        }
                        return !hasSample || sampleUuid.map(uuid -> !uuid.equals(targetPlayer.getUuid())).orElse(true);
                    }
                    return !hasSample
                            && entity instanceof PlayerBodyEntity body
                            && TargetVisibilityApi.canTargetBody(player, body);
                },
                MorphlingConstants.REAGENT_TARGET_RANGE
        ) instanceof EntityHitResult;

        CrosshairHudApi.renderStandardCrosshair(context, target);
        return CrosshairHudApi.Result.HANDLED;
    }
}
