package org.agmas.noellesroles.client.appearance.modifiers.lovers;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.modifiers.lovers.LoversConstants;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.List;
import java.util.UUID;

/**
 * 恋人准心提示接入。
 *
 * <p>左下角固定伴侣 HUD 已经迁到 {@code LoversPartnerHud} 的通用屏幕 HUD provider。
 * 这里继续留在 {@link RoleNameHudApi}，只处理“准心指向伴侣/旁观者查看恋人关系”的信息，
 * 因为这类提示本来就依赖 Wathe RoleNameRenderer 已经算好的准心目标和淡入透明度。</p>
 */
public final class LoversHudHandler {
    private LoversHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesRolesCore.id("role_name/lovers/hud"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                LoversHudHandler::render
        );
    }

    private static void render(RoleNameHudApi.Context context) {
        renderCrosshairPartnerHud(context);
    }

    private static void renderCrosshairPartnerHud(RoleNameHudApi.Context context) {
        ClientPlayerEntity player = context.player();
        PlayerEntity target = context.targetPlayer();
        if (target == null) {
            return;
        }

        WorldModifierComponent component = WorldModifierComponent.KEY.get(player.getWorld());
        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(player.getWorld());
        if (!component.isModifier(target, NoellesModifierRegistry.LOVERS)) {
            return;
        }

        if (WatheClient.isPlayerAliveAndInSurvival()
                && !LoversConstants.KNOW_IMMEDIATELY
                && component.isModifier(player, NoellesModifierRegistry.LOVERS)
                && pairComponent.arePartnersOrFallback(
                player.getUuid(),
                target.getUuid(),
                component.getAllWithModifier(NoellesModifierRegistry.LOVERS)
        )) {
            renderCenteredSmall(context, Text.translatable("hud.noellesroles.lovers.partner"));
        } else if (WatheClient.isPlayerSpectatingOrCreative()) {
            // 观战目标的另一位恋人可能还没同步到当前客户端，先跳过这一帧，不执行空值读名。
            List<UUID> lovers = component.getAllWithModifier(NoellesModifierRegistry.LOVERS);
            UUID partnerUuid = pairComponent.getPartnerOrFallback(target.getUuid(), lovers);
            if (partnerUuid == null) {
                return;
            }
            PlayerEntity loverPlayer = player.getWorld().getPlayerByUuid(partnerUuid);
            if (loverPlayer != null) {
                renderCenteredSmall(context, Text.translatable(
                        "hud.noellesroles.lovers.in_love",
                        loverPlayer.getName()
                ));
            }
        }
    }

    private static void renderCenteredSmall(RoleNameHudApi.Context context, Text text) {
        int alpha = (int) (context.nametagAlpha() * 255.0F) << 24;
        int color = LoversConstants.COLOR | alpha;

        var matrices = context.drawContext().getMatrices();
        matrices.push();
        matrices.translate(context.drawContext().getScaledWindowWidth() / 2.0F, context.drawContext().getScaledWindowHeight() / 2.0F - 35.0F, 0.0F);
        matrices.scale(0.6F, 0.6F, 1.0F);
        context.drawContext().drawTextWithShadow(
                context.renderer(),
                text,
                -context.renderer().getWidth(text) / 2,
                32,
                color
        );
        matrices.pop();
    }
}
