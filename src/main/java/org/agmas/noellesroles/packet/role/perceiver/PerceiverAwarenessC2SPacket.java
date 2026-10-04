package org.agmas.noellesroles.packet.role.perceiver;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import java.util.UUID;

/**
 * 本地玩家向服务端报告自己当前是否通过主动本能或被动描边感知指定感知者。
 *
 * <p>这个包只报告客户端独有的透视资格。距离、职业、存活状态、凝视方向和持续时间
 * 都由服务端重新校验，客户端无法通过该布尔值直接授予感知者描边。</p>
 */
public record PerceiverAwarenessC2SPacket(UUID perceiverPlayer, boolean aware) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = NoellesRolesCore.id("perceiver_awareness");
    public static final Id<PerceiverAwarenessC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, PerceiverAwarenessC2SPacket> CODEC =
            PacketCodec.of(PerceiverAwarenessC2SPacket::write, PerceiverAwarenessC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(this.perceiverPlayer);
        buf.writeBoolean(this.aware);
    }

    private static PerceiverAwarenessC2SPacket read(PacketByteBuf buf) {
        return new PerceiverAwarenessC2SPacket(buf.readUuid(), buf.readBoolean());
    }
}
