package org.agmas.noellesroles.packet.role.controller;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record ControllerPossessC2SPacket(UUID target) implements CustomPayload {
    public static final Identifier POSSESS_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "controller_possess");
    public static final Id<ControllerPossessC2SPacket> ID = new Id<>(POSSESS_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, ControllerPossessC2SPacket> CODEC;

    public ControllerPossessC2SPacket(UUID target) {
        this.target = target;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.target);
    }

    public static ControllerPossessC2SPacket read(PacketByteBuf buf) {
        return new ControllerPossessC2SPacket(buf.readUuid());
    }

    static {
        CODEC = PacketCodec.of(ControllerPossessC2SPacket::write, ControllerPossessC2SPacket::read);
    }
}
