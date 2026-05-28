package org.agmas.noellesroles.packet.role.vulture;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record VultureEatC2SPacket(UUID playerBody) implements CustomPayload {
    public static final Identifier VULTURE_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "vulture");
    public static final Id<VultureEatC2SPacket> ID = new Id<>(VULTURE_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, VultureEatC2SPacket> CODEC;

    public VultureEatC2SPacket(UUID playerBody) {
        this.playerBody = playerBody;
    }

    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.playerBody);
    }

    public static VultureEatC2SPacket read(PacketByteBuf buf) {
        return new VultureEatC2SPacket(buf.readUuid());
    }


    public UUID playerBody() {
        return this.playerBody;
    }


    static {
        CODEC = PacketCodec.of(VultureEatC2SPacket::write, VultureEatC2SPacket::read);
    }
}