package org.agmas.noellesroles.packet.role.spiritualist;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 灵术师附身期间的客户端控制同步包。
 *
 * <p>这个包只承载“这一帧灵术师想让宿主做什么”：
 * 视角、移动、跳跃、潜行、冲刺、左键、右键。
 * 真正怎样作用到宿主身上，由服务端统一裁定。</p>
 */
public record SpiritualistPossessionControlC2SPacket(
        float forward,
        float sideways,
        float yaw,
        float pitch,
        boolean jumping,
        boolean sneaking,
        boolean sprinting,
        boolean using,
        boolean attacking
) implements CustomPayload {
    public static final Identifier POSSESSION_CONTROL_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "spiritualist_possession_control");
    public static final Id<SpiritualistPossessionControlC2SPacket> ID = new Id<>(POSSESSION_CONTROL_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SpiritualistPossessionControlC2SPacket> CODEC =
            PacketCodec.of(SpiritualistPossessionControlC2SPacket::write, SpiritualistPossessionControlC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeFloat(this.forward);
        buf.writeFloat(this.sideways);
        buf.writeFloat(this.yaw);
        buf.writeFloat(this.pitch);
        buf.writeBoolean(this.jumping);
        buf.writeBoolean(this.sneaking);
        buf.writeBoolean(this.sprinting);
        buf.writeBoolean(this.using);
        buf.writeBoolean(this.attacking);
    }

    public static SpiritualistPossessionControlC2SPacket read(PacketByteBuf buf) {
        return new SpiritualistPossessionControlC2SPacket(
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
