package org.agmas.noellesroles.client.roles.dreamer;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import dev.doctor4t.wathe.api.combat.WeaponTargetingApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.dreamer.DreamerConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 幻觉注剂准心提示。
 *
 * <p>客户端这里只负责“看起来能不能扎中”的反馈；
 * 服务端收到数据包后仍会重新校验距离、存活、冷却和 TargetVisibilityApi。</p>
 */
public final class DreamerSyringeCrosshair {
    private DreamerSyringeCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/dreamer/delusion_syringe"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                DreamerSyringeCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!context.mainHandStack().isOf(ModItems.DELUSION_SYRINGE)) {
            return CrosshairHudApi.Result.PASS;
        }

        PlayerEntity player = context.player();
        ItemCooldownManager manager = player.getItemCooldownManager();
        boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
        EntityHitResult hitResult = WeaponTargetingApi.getVisibleAlivePlayerTarget(
                player,
                DreamerConstants.DELUSION_SYRINGE_TARGET_RANGE
        );

        /*
         * 准心显示使用 TARGET 语义，避免暴露只隐藏准心的伪装目标；
         * 物品松手发包使用 ATTACK 语义，服务端也按 ATTACK 语义兜底。
         */
        boolean target = hitResult != null
                && (ignoresCooldown || !manager.isCoolingDown(ModItems.DELUSION_SYRINGE));
        float progress = ignoresCooldown
                ? 1.0F
                : 1.0F - manager.getCooldownProgress(ModItems.DELUSION_SYRINGE, context.tickDelta());
        CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target, progress);
        return CrosshairHudApi.Result.HANDLED;
    }
}
