package org.agmas.noellesroles.packet.role.perceiver;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.UUID;

/** 感知者背包按钮选择单个玩家的客户端到服务端请求。 */
public record PerceiverSelectC2SPacket(UUID targetPlayer) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = NoellesRolesCore.id("perceiver_select");
    public static final Id<PerceiverSelectC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, PerceiverSelectC2SPacket> CODEC =
            PacketCodec.of(PerceiverSelectC2SPacket::write, PerceiverSelectC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(this.targetPlayer);
    }

    private static PerceiverSelectC2SPacket read(PacketByteBuf buf) {
        return new PerceiverSelectC2SPacket(buf.readUuid());
    }
}
