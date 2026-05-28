package org.agmas.noellesroles.packet.role.stalker;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

public record StalkerGazeC2SPacket(boolean gazing) implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of(Noellesroles.MOD_ID, "stalker_gaze");
    public static final Id<StalkerGazeC2SPacket> ID = new Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, StalkerGazeC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public StalkerGazeC2SPacket decode(RegistryByteBuf buf) {
            return new StalkerGazeC2SPacket(buf.readBoolean());
        }

        @Override
        public void encode(RegistryByteBuf buf, StalkerGazeC2SPacket value) {
            buf.writeBoolean(value.gazing);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}