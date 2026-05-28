package org.agmas.noellesroles.packet.role.controller;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

public record ControllerReleaseC2SPacket() implements CustomPayload {
    public static final Identifier RELEASE_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "controller_release");
    public static final Id<ControllerReleaseC2SPacket> ID = new Id<>(RELEASE_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, ControllerReleaseC2SPacket> CODEC;

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        // 无数据
    }

    public static ControllerReleaseC2SPacket read(PacketByteBuf buf) {
        return new ControllerReleaseC2SPacket();
    }

    static {
        CODEC = PacketCodec.of(ControllerReleaseC2SPacket::write, ControllerReleaseC2SPacket::read);
    }
}