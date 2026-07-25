package org.agmas.noellesroles.client.hud.modifiers.dual_personality;

import dev.doctor4t.wathe.api.client.hud.HudOverlayApi;
import dev.doctor4t.wathe.api.client.hud.HudOverlayContext;
import dev.doctor4t.wathe.api.client.hud.HudOverlayLayer;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.client.hud.NoellesHudSupport;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityClientState;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityConstants;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import java.util.UUID;

/**
 * 双重人格左下角另一人格提示。
 *
 * <p>另一人格头像/名字属于固定屏幕 HUD，不应该受准心名牌是否可见影响。
 * 旧实现挂在 RoleNameHudApi.registerExtraHud 下，停电导致 RoleNameRenderer 因光照不足提前 return 时，
 * 这里也会被跳过。迁到 HudOverlayApi 后，只有 Wathe 存活定义不满足时才隐藏。</p>
 */
public final class DualPersonalityPartnerHud {
    private DualPersonalityPartnerHud() {
    }

    public static void register() {
        HudOverlayApi.register(
                NoellesHudSupport.id("modifiers/dual_personality/partner"),
                HudOverlayLayer.MAIN_HUD,
                HudOverlayApi.DEFAULT_PRIORITY,
                DualPersonalityPartnerHud::render
        );
    }

    private static void render(HudOverlayContext context) {
        ClientPlayerEntity player = context.player();
        if (player.networkHandler == null
                || !context.aliveAndSurvival()
                || !DualPersonalityClientState.isActiveRound(player)) {
            return;
        }

        WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(player.getWorld());
        if (!modifierComponent.isModifier(player, NoellesModifierRegistry.DUAL_PERSONALITY)) {
            return;
        }

        DualPersonalityComponent dualComponent = DualPersonalityComponent.KEY.get(player.getWorld());
        UUID partnerUuid = dualComponent.getPartner(player.getUuid());
        if (partnerUuid == null) {
            // 只有词条但组件里没有配对时不渲染，避免随机分配失败时显示错误信息。
            return;
        }

        PlayerListEntry partnerInfo = player.networkHandler.getPlayerListEntry(partnerUuid);
        if (partnerInfo == null || partnerInfo.getProfile() == null) {
            return;
        }

        int textY = context.height() - 12 - getExistingLowerLeftHudOffset(player, modifierComponent);
        if (partnerInfo.getSkinTextures() != null) {
            // 画头像能让玩家更快确认另一人格是谁，尤其多人局里比只显示名字更醒目。
            PlayerSkinDrawer.draw(context.drawContext(), partnerInfo.getSkinTextures().texture(), 2, textY - 2, 12);
        }

        Text name = Text.translatable(
                "tip.noellesroles.dual_personality.partner",
                partnerInfo.getProfile().getName()
        );
        context.drawContext().drawTextWithShadow(context.textRenderer(), name, 18, textY, DualPersonalityConstants.COLOR);
    }

    private static int getExistingLowerLeftHudOffset(ClientPlayerEntity player, WorldModifierComponent modifierComponent) {
        int offset = 0;
        var role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (NoellesRoleRegistry.EXECUTIONER.equals(role)) {
            // 仇杀客目标 HUD 也占左下角一行，双重人格向上避让。
            offset += 15;
        }
        if (modifierComponent.isModifier(player, NoellesModifierRegistry.LOVERS)) {
            // 双重人格允许和恋人叠加，因此这里再给恋人提示留一行空间。
            offset += 15;
        }
        return offset;
    }
}
