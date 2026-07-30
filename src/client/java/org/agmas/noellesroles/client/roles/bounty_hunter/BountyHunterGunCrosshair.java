package org.agmas.noellesroles.client.roles.bounty_hunter;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.BountyDerringerItem;
import org.agmas.noellesroles.item.BountyPistolItem;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/**
 * 赏金枪械专用准星。
 *
 * <p>强盗手枪可以复用 Wathe 左轮 20 格准星，但赏金手枪是 15 格、赏金德林加是 7 格。
 * 这里通过 CrosshairHudApi 接管两把赏金枪，确保准星高亮范围和服务端真实射程一致。</p>
 */
public final class BountyHunterGunCrosshair {
    private BountyHunterGunCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/bounty_hunter/guns"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                BountyHunterGunCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        boolean holdingBountyPistol = context.mainHandStack().isOf(ModItems.BOUNTY_PISTOL);
        boolean holdingBountyDerringer = context.mainHandStack().isOf(ModItems.BOUNTY_DERRINGER);
        if (!holdingBountyPistol && !holdingBountyDerringer) {
            return CrosshairHudApi.Result.PASS;
        }

        ClientPlayerEntity player = context.player();
        boolean target = !player.getItemCooldownManager().isCoolingDown(context.mainHandStack().getItem())
                && (holdingBountyPistol
                ? BountyPistolItem.getGunTarget(player) instanceof EntityHitResult
                : BountyDerringerItem.getGunTarget(player) instanceof EntityHitResult);
        CrosshairHudApi.renderStandardCrosshair(context, target);
        return CrosshairHudApi.Result.HANDLED;
    }
}
