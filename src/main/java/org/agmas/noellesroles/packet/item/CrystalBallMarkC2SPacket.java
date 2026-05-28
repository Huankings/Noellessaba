package org.agmas.noellesroles.packet.item;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 水晶球标记数据包。
 *
 * <p>由于扩展模组把 client/main 源集拆开了，
 * 这里沿用 kinswathe 的思路：物品类本身只负责通过反射发包，
 * 真正的标记判定全部交给服务端执行。</p>
 *
 * @param targetId 被瞄准玩家的实体 id
 * @param offHand  本次使用是否来自副手
 */
public record CrystalBallMarkC2SPacket(int targetId, boolean offHand) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "crystal_ball_mark");
    public static final Id<CrystalBallMarkC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, CrystalBallMarkC2SPacket> CODEC = PacketCodec.of(
            CrystalBallMarkC2SPacket::write,
            CrystalBallMarkC2SPacket::read
    );

    private void write(PacketByteBuf buf) {
        buf.writeInt(this.targetId);
        buf.writeBoolean(this.offHand);
    }

    private static CrystalBallMarkC2SPacket read(PacketByteBuf buf) {
        return new CrystalBallMarkC2SPacket(buf.readInt(), buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
