package org.agmas.noellesroles.packet.role.myers;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** G 键吸收恶意值的按下/松开包。 */
public record MyersAbsorbC2SPacket(boolean absorbing) implements CustomPayload {
    public static final Identifier IDENTIFIER = NoellesRolesCore.id("myers_absorb");
    public static final Id<MyersAbsorbC2SPacket> ID = new Id<>(IDENTIFIER);
    public static final PacketCodec<RegistryByteBuf, MyersAbsorbC2SPacket> CODEC = new PacketCodec<>() {
        @Override public MyersAbsorbC2SPacket decode(RegistryByteBuf buf) { return new MyersAbsorbC2SPacket(buf.readBoolean()); }
        @Override public void encode(RegistryByteBuf buf, MyersAbsorbC2SPacket value) { buf.writeBoolean(value.absorbing); }
    };
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
