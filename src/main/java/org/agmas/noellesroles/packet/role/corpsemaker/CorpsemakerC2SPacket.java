package org.agmas.noellesroles.packet.role.corpsemaker;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

public record CorpsemakerC2SPacket(UUID target, String deathReason, String roleIdentifier) implements CustomPayload {
    public static final Identifier CORPSEMAKER_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "corpsemaker");
    public static final CustomPayload.Id<CorpsemakerC2SPacket> ID = new CustomPayload.Id<>(CORPSEMAKER_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, CorpsemakerC2SPacket> CODEC;

    public CorpsemakerC2SPacket(UUID target, String deathReason, String roleIdentifier) {
        this.target = target;
        this.deathReason = deathReason;
        this.roleIdentifier = roleIdentifier;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.target);
        buf.writeString(this.deathReason);
        buf.writeString(this.roleIdentifier);
    }

    public static CorpsemakerC2SPacket read(PacketByteBuf buf) {
        return new CorpsemakerC2SPacket(buf.readUuid(), buf.readString(), buf.readString());
    }

    static {
        CODEC = PacketCodec.of(CorpsemakerC2SPacket::write, CorpsemakerC2SPacket::read);
    }
}