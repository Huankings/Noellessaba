package org.agmas.noellesroles.client.roles.assassin;

import dev.doctor4t.wathe.api.client.gui.CrosshairHudApi;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.util.hit.EntityHitResult;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.item.BayonetItem;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

/**
 * 刺刀专用准心。
 *
 * <p>Wathe 原版准心只会识别 {@code wathe:knife}，因此这里单独为刺刀补一套和匕首相同的
 * 冷却/锁定显示。现在通过 Wathe 的 {@link CrosshairHudApi} 接入，不再 mixin CrosshairRenderer。</p>
 */
public final class AssassinBayonetCrosshair {
    private AssassinBayonetCrosshair() {
    }

    public static void register() {
        CrosshairHudApi.registerProvider(
                NoellesRolesCore.id("crosshair/assassin/bayonet"),
                CrosshairHudApi.DEFAULT_PRIORITY,
                AssassinBayonetCrosshair::render
        );
    }

    private static @NotNull CrosshairHudApi.Result render(@NotNull CrosshairHudApi.Context context) {
        if (!context.mainHandStack().isOf(ModItems.BAYONET)) {
            return CrosshairHudApi.Result.PASS;
        }

        ClientPlayerEntity player = context.player();
        ItemCooldownManager manager = player.getItemCooldownManager();
        boolean target = !manager.isCoolingDown(ModItems.BAYONET)
                && BayonetItem.getBayonetTarget(player) instanceof EntityHitResult;
        float progress = 1.0F - manager.getCooldownProgress(ModItems.BAYONET, context.tickDelta());
        CrosshairHudApi.renderKnifeProgressCrosshair(context, target, target, progress);
        return CrosshairHudApi.Result.HANDLED;
    }
}
