package org.agmas.noellesroles.packet.role.stalker;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

public record StalkerDashC2SPacket(boolean charging) implements CustomPayload {
    public static final Identifier IDENTIFIER = Identifier.of(Noellesroles.MOD_ID, "stalker_dash");
    public static final Id<StalkerDashC2SPacket> ID = new Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, StalkerDashC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public StalkerDashC2SPacket decode(RegistryByteBuf buf) {
            return new StalkerDashC2SPacket(buf.readBoolean());
        }

        @Override
        public void encode(RegistryByteBuf buf, StalkerDashC2SPacket value) {
            buf.writeBoolean(value.charging);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}