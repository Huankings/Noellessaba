package org.agmas.noellesroles.packet.modifiers.dual_personality;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 同步客户端当前绑定的双重人格切换键显示文本。
 */
public record DualPersonalitySwitchKeyLabelC2SPacket(String keyLabel) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = Identifier.of(NoellesRolesCore.MOD_ID, "dual_personality_switch_key_label");
    public static final Id<DualPersonalitySwitchKeyLabelC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, DualPersonalitySwitchKeyLabelC2SPacket> CODEC =
            PacketCodec.of(DualPersonalitySwitchKeyLabelC2SPacket::write, DualPersonalitySwitchKeyLabelC2SPacket::read);

    private void write(PacketByteBuf buf) {
        buf.writeString(this.keyLabel, 64);
    }

    private static DualPersonalitySwitchKeyLabelC2SPacket read(PacketByteBuf buf) {
        return new DualPersonalitySwitchKeyLabelC2SPacket(buf.readString(64));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
