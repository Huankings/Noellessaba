package org.agmas.noellesroles.packet.role.spiritualist;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/**
 * 灵术师附身期间，服务器发给“灵术师本人客户端 / 被附身者客户端”的视图同步包。
 *
 * <p>它和 C2S 的控制包职责正好相反：</p>
 * <p>1. C2S 包表达“灵术师这帧想怎么操作”；</p>
 * <p>2. 这个 S2C 包表达“服务器最终认定宿主当前实际处于什么位置和朝向”。</p>
 *
 * <p>这样客户端就能基于一份稳定的服务端真值，做本地相机层和平滑视图层，
 * 不必再完全依赖 requestTeleport 那种很生硬的强制纠正。</p>
 */
public record SpiritualistPossessionViewS2CPacket(
        UUID targetPlayer,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        float yaw,
        float pitch,
        float headYaw,
        float bodyYaw,
        String poseName,
        float eyeHeight,
        boolean sprinting,
        boolean sneaking,
        boolean onGround
) implements CustomPayload {
    public static final Identifier POSSESSION_VIEW_PAYLOAD_ID =
            Identifier.of(Noellesroles.MOD_ID, "spiritualist_possession_view");
    public static final Id<SpiritualistPossessionViewS2CPacket> ID = new Id<>(POSSESSION_VIEW_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, SpiritualistPossessionViewS2CPacket> CODEC =
            PacketCodec.of(SpiritualistPossessionViewS2CPacket::write, SpiritualistPossessionViewS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(this.targetPlayer);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeDouble(this.velocityX);
        buf.writeDouble(this.velocityY);
        buf.writeDouble(this.velocityZ);
        buf.writeFloat(this.yaw);
        buf.writeFloat(this.pitch);
        buf.writeFloat(this.headYaw);
        buf.writeFloat(this.bodyYaw);
        buf.writeString(this.poseName);
        buf.writeFloat(this.eyeHeight);
        buf.writeBoolean(this.sprinting);
        buf.writeBoolean(this.sneaking);
        buf.writeBoolean(this.onGround);
    }

    public static SpiritualistPossessionViewS2CPacket read(PacketByteBuf buf) {
        return new SpiritualistPossessionViewS2CPacket(
                buf.readUuid(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readString(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }
}
