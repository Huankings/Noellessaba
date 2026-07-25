package org.agmas.noellesroles.client.modifiers.dual_personality;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 双重人格外观解析工具。
 *
 * <p>这里专门放“根据 UUID 取皮肤 / 取名字”的纯工具逻辑，
 * 不再依赖召集者的组件或字段，避免双活外观在类加载阶段间接碰到其它职业代码。</p>
 */
public final class DualPersonalityAppearanceResolver {

    private DualPersonalityAppearanceResolver() {
    }

    public static @Nullable SkinTextures resolveSkinForUuid(AbstractClientPlayerEntity player, UUID disguiseUuid) {
        if (disguiseUuid == null) {
            return null;
        }

        /*
         * 必须按 UUID 回查“原始皮肤”，不能直接读当前实体的 getSkinTextures()。
         * 否则如果某个外观又被更高优先级规则覆盖，就会把当前伪装层再套一层。
         */
        return PlayerAppearanceApi.resolveOriginalSkinTextures(disguiseUuid, true);
    }

    public static @Nullable Text resolveDisguiseName(PlayerEntity viewer, @Nullable UUID disguiseUuid) {
        if (disguiseUuid == null) {
            return null;
        }

        // 自己伪装成自己时，直接显示本名。
        if (disguiseUuid.equals(viewer.getUuid())) {
            return viewer.getDisplayName();
        }

        // 优先从当前世界找实体名字，避免刚进局时玩家列表还没更新。
        PlayerEntity worldPlayer = viewer.getWorld().getPlayerByUuid(disguiseUuid);
        if (worldPlayer != null) {
            return worldPlayer.getDisplayName();
        }

        // 再从玩家列表缓存里找名字。
        PlayerListEntry entry = resolvePlayerInfo(disguiseUuid);
        if (entry != null && entry.getProfile() != null) {
            return Text.literal(entry.getProfile().getName());
        }

        return null;
    }

    public static @Nullable PlayerListEntry resolvePlayerInfo(UUID uuid) {
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

        PlayerEntity worldPlayer = client.world.getPlayerByUuid(uuid);
        if (worldPlayer instanceof AbstractClientPlayerEntity abstractClientPlayer) {
            return abstractClientPlayer;
        }
        return null;
    }
}
