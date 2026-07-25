package org.agmas.noellesroles.client.hud;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.client.hud.HudOverlayApi;
import dev.doctor4t.wathe.api.client.hud.HudOverlayContext;
import dev.doctor4t.wathe.api.client.hud.HudOverlayLayer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * NoellesRoles 通用 HUD 接入辅助。
 *
 * <p>这里只放坐标绘制、玩家名兜底和注册样板这类机械重复代码；
 * 各职业应该继续把自己的状态判定和文本拼装放在 {@code client.roles.<role>} 目录，
 * 避免迁移到 Wathe HUD API 后又形成一个难维护的大类。</p>
 */
public final class NoellesHudSupport {
    private NoellesHudSupport() {
    }

    public static Identifier id(String path) {
        return NoellesRolesCore.id("hud/" + path);
    }

    public static void registerAliveRole(String path, Role role, HudOverlayApi.HudOverlayRenderer renderer) {
        HudOverlayApi.registerAliveRole(
                id(path),
                HudOverlayLayer.MAIN_HUD,
                HudOverlayApi.DEFAULT_PRIORITY,
                role,
                renderer
        );
    }

    public static void drawBottomRightLine(@NotNull HudOverlayContext context, @NotNull Text line, int color) {
        TextRenderer renderer = context.textRenderer();
        DrawContext drawContext = context.drawContext();
        int y = context.height() - renderer.getWrappedLinesHeight(line, 999999);
        drawContext.drawTextWithShadow(renderer, line, context.width() - renderer.getWidth(line), y, color);
    }

    public static void drawBottomRightLines(@NotNull HudOverlayContext context, @NotNull List<Text> lines, int color) {
        TextRenderer renderer = context.textRenderer();
        DrawContext drawContext = context.drawContext();
        int y = context.height();
        for (int index = lines.size() - 1; index >= 0; index--) {
            Text line = lines.get(index);
            y -= renderer.getWrappedLinesHeight(line, 999999);
            drawContext.drawTextWithShadow(renderer, line, context.width() - renderer.getWidth(line), y, color);
        }
    }

    public static Text resolvePlayerName(@NotNull ClientPlayerEntity player,
                                         UUID uuid,
                                         @NotNull Text fallback) {
        if (uuid == null) {
            return fallback;
        }
        if (uuid.equals(player.getUuid())) {
            return player.getDisplayName();
        }
        PlayerListEntry entry = player.networkHandler.getPlayerListEntry(uuid);
        if (entry != null) {
            return Text.literal(entry.getProfile().getName());
        }
        return fallback;
    }
}
