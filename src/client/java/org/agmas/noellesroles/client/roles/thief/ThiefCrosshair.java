package org.agmas.noellesroles.client.roles.thief;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.agmas.noellesroles.AbilityPlayerComponent;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.thief.ThiefConstants;
import org.agmas.noellesroles.roles.thief.ThiefInteractionHandler;
import org.jetbrains.annotations.NotNull;

public final class ThiefCrosshair {
    private static final Identifier THIEF_READY = NoellesRolesCore.id("hud/thief_ready");
    private static final Identifier THIEF_PROGRESS_FILL = NoellesRolesCore.id("hud/thief_progress_fill");
    private static final Identifier THIEF_PROGRESS_BACKGROUND = NoellesRolesCore.id("hud/thief_progress_background");

    private ThiefCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/thief/steal"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                ThiefCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!shouldShowThiefCrosshair(context.client(), context.player())) {
            return CrosshairHudApi.Result.PASS;
        }

        AbilityPlayerComponent ability = AbilityPlayerComponent.KEY.get(context.player());
        boolean ready = ability.cooldown <= 0;
        float progress = 1.0F - ((float) ability.cooldown / (float) ThiefConstants.STEAL_COOLDOWN_TICKS);
        CrosshairHudApi.renderIconProgressCrosshair(
                context,
                true,
                ready,
                progress,
                THIEF_READY,
                THIEF_PROGRESS_BACKGROUND,
                THIEF_PROGRESS_FILL
        );
        return CrosshairHudApi.Result.HANDLED;
    }

    private static boolean shouldShowThiefCrosshair(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player) {
        if (!GameFunctions.isPlayerAliveAndSurvival(player)) {
            return false;
        }
        if (!GameWorldComponent.KEY.get(player.getWorld()).isRole(player, NoellesRoleRegistry.THIEF)) {
            return false;
        }
        if (!player.getMainHandStack().isEmpty()) {
            return false;
        }

        HitResult hitResult = client.crosshairTarget;
        if (hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity target) {
            return GameFunctions.isPlayerAliveAndSurvival(target)
                    /*
                     * 尸体伪装玩家在 TargetVisibilityApi 中声明“不要被当作准心目标”。
                     * 小偷空手准心直接读取 client.crosshairTarget，如果不补这层过滤，
                     * 玩家对准伪装尸体时仍会出现偷窃图标，从而暴露这其实是活人。
                     */
                    && TargetVisibilityApi.canTargetPlayer(player, target)
                    && player.squaredDistanceTo(target) <= ThiefConstants.CLIENT_STEAL_RANGE * ThiefConstants.CLIENT_STEAL_RANGE
                    && ThiefInteractionHandler.validateDistance(player, target);
        }
        return false;
    }
}
