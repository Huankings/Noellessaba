package org.agmas.noellesroles.client.appearance;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.agmas.noellesroles.client.renderer.DisguiseRenderHelper;

/**
 * 客户端本地玩家原始皮肤缓存。
 *
 * <p>变形渲染会临时覆盖本地玩家外观，因此不能把当前渲染结果当作原始皮肤。
 * 本类只保存和刷新原始皮肤，具体伪装规则仍由 {@link DisguiseRenderHelper} 负责。</p>
 */
public final class NoellesAppearanceClientState {
    private static SkinTextures localPlayerOriginalSkinTextures;

    private NoellesAppearanceClientState() {
    }

    public static SkinTextures getLocalPlayerOriginalSkinTextures() {
        return localPlayerOriginalSkinTextures;
    }

    public static void refresh(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }
        // 有外观覆盖时暂停刷新，避免把伪装皮肤写回“原始皮肤”缓存。
        if (DisguiseRenderHelper.resolveAppearanceOverrideSkinTextures(client.player) != null) {
            return;
        }
        if (client.player.networkHandler != null) {
            PlayerListEntry entry = client.player.networkHandler.getPlayerListEntry(client.player.getUuid());
            if (entry != null) {
                localPlayerOriginalSkinTextures = entry.getSkinTextures();
                return;
            }
        }
        // 玩家列表尚未同步时，用本地实体的当前皮肤作为最后兜底。
        localPlayerOriginalSkinTextures = client.player.getSkinTextures();
    }

    public static void reset() {
        localPlayerOriginalSkinTextures = null;
    }
}
