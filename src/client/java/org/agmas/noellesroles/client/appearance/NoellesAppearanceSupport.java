package org.agmas.noellesroles.client.appearance;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.client.NoellesrolesClient;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * NoellesRoles 外观接入的公共工具。
 *
 * <p>这里放“解析原始皮肤/名字”这种和具体职业无关的逻辑，避免每个职业类复制一份缓存兜底。</p>
 */
public final class NoellesAppearanceSupport {
    private NoellesAppearanceSupport() {
    }

    public static SkinTextures resolveLocalOriginalSkin(ClientPlayerEntity localPlayer) {
        /*
         * 灵术师出窍时需要稳定显示自己的原始皮肤。
         * 如果 NoellesrolesClient 已经在登录时缓存过本地原皮，优先使用缓存；
         * 缓存还没准备好时，再让 Wathe 按 UUID 从玩家列表 / Wathe 缓存 / 默认皮肤逐层兜底。
         */
        if (NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES != null) {
            return NoellesrolesClient.LOCAL_PLAYER_ORIGINAL_SKIN_TEXTURES;
        }
        return PlayerAppearanceApi.resolveOriginalSkinTextures(localPlayer.getUuid(), true);
    }

    public static Text resolveNameFromUuid(PlayerEntity viewer, @Nullable UUID targetUuid, Text fallback) {
        if (targetUuid == null) {
            return fallback;
        }
        if (targetUuid.equals(viewer.getUuid())) {
            return fallback;
        }

        /*
         * 准心名字要尽量和伪装皮肤来自同一个 UUID。
         * 先读本地玩家和世界内活玩家，保证在线目标的显示名不会因为缓存滞后而变旧。
         */
        ClientPlayerEntity localPlayer = MinecraftClient.getInstance().player;
        if (localPlayer != null && targetUuid.equals(localPlayer.getUuid())) {
            return localPlayer.getDisplayName();
        }

        PlayerEntity livePlayer = viewer.getWorld().getPlayerByUuid(targetUuid);
        if (livePlayer != null) {
            return livePlayer.getDisplayName();
        }

        /*
         * 目标离线、二次进服或资源重载早期，只能依赖 Wathe 的玩家列表缓存。
         * 缓存也没有时给出稳定文本，避免 HUD 渲染线程因为空名字崩溃。
         */
        String cachedName = PlayerAppearanceApi.resolveOriginalPlayerName(targetUuid);
        return cachedName == null ? Text.literal("Unknown Player") : Text.literal(cachedName);
    }

    public static Identifier id(String path) {
        return Identifier.of(Noellesroles.MOD_ID, path);
    }
}
