package org.agmas.noellesroles.packet.role.goddess;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record GoddessC2SPacket(UUID target) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "goddess");
    public static final Id<GoddessC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, GoddessC2SPacket> CODEC = PacketCodec.of(
            GoddessC2SPacket::write,
            GoddessC2SPacket::read
    );

    private void write(PacketByteBuf buf) {
        buf.writeUuid(target);
    }

    private static GoddessC2SPacket read(PacketByteBuf buf) {
        return new GoddessC2SPacket(buf.readUuid());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}