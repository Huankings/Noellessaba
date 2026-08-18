package org.agmas.noellesroles.client.roles.shadow_jester;

import dev.doctor4t.wathe.api.client.hud.HudOverlayApi;
import dev.doctor4t.wathe.api.client.hud.HudOverlayContext;
import dev.doctor4t.wathe.api.client.hud.HudOverlayLayer;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;

import java.util.UUID;

/**
 * 影子小丑左下角另一半 HUD。
 */
public final class ShadowJesterPartnerHud {
    private ShadowJesterPartnerHud() {
    }

    public static void register() {
        HudOverlayApi.register(
                NoellesHudSupport.id("roles/shadow_jester/partner"),
                HudOverlayLayer.MAIN_HUD,
                HudOverlayApi.DEFAULT_PRIORITY,
                ShadowJesterPartnerHud::render
        );
    }

    private static void render(HudOverlayContext context) {
        ClientPlayerEntity player = context.player();
        if (player.networkHandler == null
                || !context.isAliveRole(NoellesRoleRegistry.SHADOW_JESTER)) {
            return;
        }

        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(player.getWorld());
        UUID partnerUuid = component.getPartner(player.getUuid());
        if (partnerUuid == null) {
            return;
        }

        PlayerListEntry partnerInfo = player.networkHandler.getPlayerListEntry(partnerUuid);
        String partnerName = partnerInfo != null && partnerInfo.getProfile() != null
                ? partnerInfo.getProfile().getName()
                : partnerUuid.toString();

        int textY = context.height() - 12;
        int textX = 18;
        /*
         * 头像依赖玩家列表皮肤资料；刚进服同步未完成时先画名字兜底，
         * 下一帧资料到齐后自然补上头像，不需要服务端额外发包。
         */
        if (partnerInfo != null && partnerInfo.getSkinTextures() != null) {
            PlayerSkinDrawer.draw(context.drawContext(), partnerInfo.getSkinTextures().texture(), 2, textY - 2, 12);
        }

        context.drawContext().drawTextWithShadow(
                context.textRenderer(),
                Text.translatable("tip.noellesroles.shadow_jester.partner", partnerName),
                textX,
                textY,
                ShadowJesterConstants.ROLE_COLOR
        );
    }
}
