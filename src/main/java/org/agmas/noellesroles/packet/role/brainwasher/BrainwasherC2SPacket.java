package org.agmas.noellesroles.packet.role.brainwasher;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record BrainwasherC2SPacket(UUID target) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "brainwasher");
    public static final Id<BrainwasherC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, BrainwasherC2SPacket> CODEC = PacketCodec.of(
            BrainwasherC2SPacket::write,
            BrainwasherC2SPacket::read
    );

    private void write(PacketByteBuf buf) {
        buf.writeUuid(target);
    }

    private static BrainwasherC2SPacket read(PacketByteBuf buf) {
        return new BrainwasherC2SPacket(buf.readUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}