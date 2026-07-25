package org.agmas.noellesroles.client.hud.modifiers.lovers;

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
import org.agmas.noellesroles.modifiers.lovers.LoversConstants;
import org.agmas.noellesroles.modifiers.lovers.LoversPairComponent;
import org.agmas.noellesroles.registry.NoellesModifierRegistry;
import org.agmas.noellesroles.registry.NoellesRoleRegistry;

import java.util.List;
import java.util.UUID;

/**
 * 恋人左下角伴侣 HUD。
 *
 * <p>这个 HUD 是固定屏幕信息，不依赖准心目标；因此不能挂在 RoleNameHudApi 上。
 * Wathe 的 RoleNameRenderer 在停电/黑暗环境会提前返回，如果继续放在那里，
 * 左下角伴侣头像和名字也会被一起跳过。这里改走通用 HudOverlayApi，
 * 并用 context.aliveAndSurvival() 统一遵循 Wathe 的“存活玩家才渲染”规则。</p>
 */
public final class LoversPartnerHud {
    private LoversPartnerHud() {
    }

    public static void register() {
        HudOverlayApi.register(
                NoellesHudSupport.id("modifiers/lovers/partner"),
                HudOverlayLayer.MAIN_HUD,
                HudOverlayApi.DEFAULT_PRIORITY,
                LoversPartnerHud::render
        );
    }

    private static void render(HudOverlayContext context) {
        ClientPlayerEntity player = context.player();
        if (player.networkHandler == null || !context.aliveAndSurvival()) {
            // 客户端切世界、重连或旁观状态切换时，networkHandler 可能短暂不可用。
            return;
        }

        WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(player.getWorld());
        if (!modifierComponent.isModifier(player, NoellesModifierRegistry.LOVERS)) {
            return;
        }

        /*
         * 多对恋人必须从配对组件里精确取自己的伴侣。
         * getPartnerOrFallback 只在旧式单对数据下做兼容兜底，避免把所有 LOVERS 都画成伴侣。
         */
        LoversPairComponent pairComponent = LoversPairComponent.KEY.get(player.getWorld());
        List<UUID> lovers = modifierComponent.getAllWithModifier(NoellesModifierRegistry.LOVERS);
        UUID partnerUuid = pairComponent.getPartnerOrFallback(player.getUuid(), lovers);
        if (partnerUuid == null) {
            return;
        }

        int textY = context.height() - 12;
        int textX = 18;

        Text name;
        if (!LoversConstants.KNOW_IMMEDIATELY) {
            name = Text.translatable("hud.noellesroles.lovers.notification");
            textX -= 14;
        } else {
            PlayerListEntry partnerInfo = player.networkHandler.getPlayerListEntry(partnerUuid);
            if (partnerInfo == null || partnerInfo.getProfile() == null) {
                // 玩家列表资料可能晚于世界组件同步；跳过这一帧，下一帧资料到齐后再显示。
                return;
            }

            name = Text.translatable("tip.noellesroles.lovers.partner", partnerInfo.getProfile().getName());
            if (partnerInfo.getSkinTextures() != null) {
                PlayerSkinDrawer.draw(context.drawContext(), partnerInfo.getSkinTextures().texture(), 2, textY - 2, 12);
            }
        }

        if (NoellesRoleRegistry.EXECUTIONER.equals(GameWorldComponent.KEY.get(player.getWorld()).getRole(player))) {
            // 仇杀客目标 HUD 也占左下角第一行，恋人提示向上避让。
            textY -= 15;
        }

        context.drawContext().drawTextWithShadow(context.textRenderer(), name, textX, textY, LoversConstants.COLOR);
    }
}
