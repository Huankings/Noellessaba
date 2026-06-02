package org.agmas.noellesroles.packet.role.operator;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

import java.util.UUID;

/**
 * 接线员背包界面的双目标选择发包。
 *
 * <p>这里不区分“接线”还是“广播”，
 * 服务端只看 first / second 是否相同来分流逻辑：</p>
 * <p>1. 不同玩家：接线</p>
 * <p>2. 相同玩家：广播</p>
 */
public record OperatorC2SPacket(UUID firstPlayer, UUID secondPlayer) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "operator");
    public static final Id<OperatorC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, OperatorC2SPacket> CODEC =
            PacketCodec.of(OperatorC2SPacket::write, OperatorC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(this.firstPlayer);
        buf.writeUuid(this.secondPlayer);
    }

    private static OperatorC2SPacket read(PacketByteBuf buf) {
        return new OperatorC2SPacket(buf.readUuid(), buf.readUuid());
    }
}
