package org.agmas.noellesroles.client.ui.common;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 背包/选人界面里的玩家头像统一从这里解析。
 *
 * <p>这层和世界内玩家渲染要严格分开：</p>
 * 1. 世界内渲染现在允许被变形职业覆写 {@code getSkinTextures()}，
 *    这样 Morphling / Controller / Coroner 才能真正切换成目标的 slim/classic 模型。
 * 2. 但背包头像如果也去读“实时实体的 getSkinTextures()”，
 *    就会把伪装后的皮肤直接泄露到选人头像里，等于把身份提示送到 UI 上。
 *
 * <p>因此这里故意只走“原始皮肤缓存链”：</p>
 * 优先使用传进来的 PlayerListEntry，其次查本地 Tab 列表，再兜底 Wathe 的缓存，
 * 最后退回默认皮肤。整个过程完全不读实时实体皮肤，确保头像不会随着变形实时改变。
 */
public final class PlayerHeadTextureHelper {

    private PlayerHeadTextureHelper() {
    }

    public static SkinTextures resolveStableSkinTextures(UUID targetUuid, @Nullable PlayerListEntry preferredEntry) {
        if (preferredEntry != null) {
            return preferredEntry.getSkinTextures();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity localPlayer = client.player;
        if (localPlayer != null
                && targetUuid.equals(localPlayer.getUuid())
                && org.agmas.noellesroles.client.NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES != null) {
            // 背包头像和世界内变形解析保持一致：
            // 如果目标就是本地玩家自己，也必须固定回到“本地玩家原始皮肤缓存”，
            // 不能把自己当前伪装后的显示皮肤又反向带进头像系统。
            return org.agmas.noellesroles.client.NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES;
        }

        if (localPlayer != null && localPlayer.networkHandler != null) {
            PlayerListEntry playerListEntry = localPlayer.networkHandler.getPlayerListEntry(targetUuid);
            if (playerListEntry != null) {
                return playerListEntry.getSkinTextures();
            }
        }

        if (WatheClient.PLAYER_ENTRIES_CACHE != null) {
            // 背包头像和世界渲染一样，也要兼容“缓存 key 还在但 value 暂时为空”的重连阶段。
            PlayerListEntry cachedEntry = WatheClient.PLAYER_ENTRIES_CACHE.get(targetUuid);
            if (cachedEntry != null) {
                return cachedEntry.getSkinTextures();
            }
        }

        return DefaultSkinHelper.getSkinTextures(targetUuid);
    }
}
