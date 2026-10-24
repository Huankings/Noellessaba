package org.agmas.noellesroles.packet.role.party_animal;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/** 派对狂背包按钮切换升调/降调类型的客户端包。 */
public record PartyAnimalTypeC2SPacket(int type) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = NoellesRolesCore.id("party_animal_type");
    public static final Id<PartyAnimalTypeC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, PartyAnimalTypeC2SPacket> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeByte(value.type()),
            buf -> new PartyAnimalTypeC2SPacket(buf.readByte())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
