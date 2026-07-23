package org.agmas.noellesroles.client.roles.convener;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceSupport;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 召集者客户端伪装解析工具。
 */
public final class ConvenerDisguiseResolver {
    private ConvenerDisguiseResolver() {
    }

    public static @Nullable SkinTextures resolveSkinForUuid(UUID disguiseUuid) {
        if (disguiseUuid == null) {
            return null;
        }

        /*
         * 必须读 UUID 对应的“原始皮肤”，不能读目标玩家当前实体皮肤。
         * 因为目标实体也可能已经被其它伪装规则覆盖，直接读实体会把伪装层层套进去。
         */
        return PlayerAppearanceApi.resolveOriginalSkinTextures(disguiseUuid, true);
    }

    public static @Nullable Text resolveDisguiseName(PlayerEntity viewer, @Nullable UUID disguiseUuid) {
        if (disguiseUuid == null) {
            return null;
        }
        return NoellesAppearanceSupport.resolveNameFromUuid(viewer, disguiseUuid, viewer.getDisplayName());
    }

    public static @Nullable PlayerListEntry resolvePlayerListEntry(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            return client.player.networkHandler.getPlayerListEntry(uuid);
        }
        return null;
    }

    public static @Nullable AbstractClientPlayerEntity resolveLivePlayer(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return null;
        }
        PlayerEntity player = client.world.getPlayerByUuid(uuid);
        return player instanceof AbstractClientPlayerEntity abstractClientPlayer ? abstractClientPlayer : null;
    }
}
