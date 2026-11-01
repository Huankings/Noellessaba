package org.agmas.noellesroles.packet.role.thief;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** 小偷背包二级界面发送的悦灵召唤请求。 */
public record ThiefAllaySummonC2SPacket(UUID targetUuid, int count) implements CustomPayload {
    public static final Id<ThiefAllaySummonC2SPacket> ID = new Id<>(NoellesRolesCore.id("thief_allay_summon"));
    public static final PacketCodec<RegistryByteBuf, ThiefAllaySummonC2SPacket> CODEC = PacketCodec.tuple(
            net.minecraft.util.Uuids.PACKET_CODEC,
            ThiefAllaySummonC2SPacket::targetUuid,
            PacketCodecs.INTEGER,
            ThiefAllaySummonC2SPacket::count,
            ThiefAllaySummonC2SPacket::new
    );

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }
}
