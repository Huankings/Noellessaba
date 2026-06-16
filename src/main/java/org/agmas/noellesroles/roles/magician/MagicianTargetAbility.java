package org.agmas.noellesroles.roles.magician;

import com.mojang.authlib.GameProfile;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.role.morphling.MorphC2SPacket;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 魔术师头像选人处理。
 *
 * <p>和风灵师一样，这里不设置额外选择冷却，切换目标本身是无冷却的。</p>
 */
public final class MagicianTargetAbility {

    private MagicianTargetAbility() {
    }

    public static void handle(MorphC2SPacket payload, ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.MAGICIAN)) {
            return;
        }

        UUID targetUuid = payload.player();
        String targetName = resolveTargetName(player, targetUuid);
        GameProfile profileSnapshot = copyProfileSnapshot(player, targetUuid, targetName);
        MagicianPlayerComponent.KEY.get(player).setSelectedTarget(targetUuid, targetName, profileSnapshot);
    }

    private static String resolveTargetName(ServerPlayerEntity player, UUID targetUuid) {
        if (targetUuid.equals(player.getUuid())) {
            return player.getGameProfile().getName();
        }

        ServerPlayerEntity onlineTarget = player.getServer().getPlayerManager().getPlayer(targetUuid);
        if (onlineTarget != null) {
            return onlineTarget.getGameProfile().getName();
        }

        UserCache userCache = player.getServer().getUserCache();
        return userCache.getByUuid(targetUuid)
                .map(GameProfile::getName)
                .orElse(targetUuid.toString().substring(0, 8));
    }

    private static @Nullable GameProfile copyProfileSnapshot(ServerPlayerEntity player, UUID targetUuid, String fallbackName) {
        GameProfile source = targetUuid.equals(player.getUuid())
                ? player.getGameProfile()
                : player.getServer().getPlayerManager().getPlayer(targetUuid) != null
                ? player.getServer().getPlayerManager().getPlayer(targetUuid).getGameProfile()
                : player.getServer().getUserCache().getByUuid(targetUuid).orElse(null);

        if (source == null) {
            return new GameProfile(targetUuid, fallbackName);
        }

        GameProfile copy = new GameProfile(source.getId(), source.getName());
        source.getProperties().forEach((key, property) -> copy.getProperties().put(key, property));
        return copy;
    }
}
