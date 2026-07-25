package org.agmas.noellesroles.client.appearance.modifiers.dual_personality;

import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.agmas.noellesroles.client.modifiers.dual_personality.DualPersonalityClientState;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityComponent;
import org.agmas.noellesroles.modifiers.dual_personality.DualPersonalityConstants;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.UUID;

/**
 * 双重人格左下角另一人格提示。
 */
public final class DualPersonalityHudHandler {
    private DualPersonalityHudHandler() {
    }

    public static void register() {
        RoleNameHudApi.registerExtraHud(
                NoellesRolesCore.id("role_name/dual_personality/partner_hud"),
                RoleNameHudApi.DEFAULT_PRIORITY,
                DualPersonalityHudHandler::render
        );
    }

    private static void render(RoleNameHudApi.Context context) {
        /*
         * Wathe 的角色 HUD 已经渲染完成后，再在左下角追加“另一人格”提示。
         * 这里不改原 HUD 主流程，降低和其它职业/词条 HUD 的冲突概率。
         */
        ClientPlayerEntity player = context.player();
        if (player.networkHandler == null
                || !DualPersonalityClientState.isActiveRound(player)
                || !WatheClient.isPlayerAliveAndInSurvival()) {
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

        int textY = context.drawContext().getScaledWindowHeight() - 12 - getExistingLowerLeftHudOffset(player, modifierComponent);
        if (partnerInfo.getSkinTextures() != null) {
            // 画头像能让玩家更快确认另一人格是谁，尤其多人局里比只显示名字更醒目。
            PlayerSkinDrawer.draw(context.drawContext(), partnerInfo.getSkinTextures().texture(), 2, textY - 2, 12);
        }

        Text name = Text.translatable(
                "tip.noellesroles.dual_personality.partner",
                partnerInfo.getProfile().getName()
        );
        context.drawContext().drawTextWithShadow(context.renderer(), name, 18, textY, DualPersonalityConstants.COLOR);
    }

    private static int getExistingLowerLeftHudOffset(ClientPlayerEntity player, WorldModifierComponent modifierComponent) {
        int offset = 0;
        var role = GameWorldComponent.KEY.get(player.getWorld()).getRole(player);
        if (NoellesRoleRegistry.EXECUTIONER.equals(role)) {
            // 处刑人 HUD 也占左下角一行，双重人格向上避让。
            offset += 15;
        }
        if (modifierComponent.isModifier(player, NoellesModifierRegistry.LOVERS)) {
            // 双重人格允许和恋人叠加，因此这里再给恋人提示留一行空间。
            offset += 15;
        }
        return offset;
    }
}
