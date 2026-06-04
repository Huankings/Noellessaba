package org.agmas.noellesroles.packet.host;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;

/**
 * 通用能力键 C2S 数据包。
 *
 * <p>大多数职业只需要告诉服务端“我按下了能力键”，
 * 因此默认 targetId 会是 -1。
 *
 * <p>但像天使这种“能力模式取决于当前准心是否锁定玩家”的职业，
 * 如果仍然让服务端自己在收到包后再重新射线判定，
 * 就很容易因为目标横向移动而把原本的“守护”误判成“安抚”。
 *
 * <p>因此这里补一个可选的 {@code targetId} 字段：
 * 客户端可以把自己这一帧已经命中的实体 id 带给服务端，
 * 服务端只做合法性 / 距离校验，不再重新做那次容易丢目标的瞄准判定。</p>
 */
public record AbilityC2SPacket(int targetId) implements CustomPayload {
    public static final Identifier ABILITY_PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "ability");
    public static final Id<AbilityC2SPacket> ID = new Id<>(ABILITY_PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, AbilityC2SPacket> CODEC;

    public AbilityC2SPacket() {
        this(-1);
    }

    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(RegistryByteBuf buf) {
        buf.writeInt(this.targetId);
    }

    public static AbilityC2SPacket read(RegistryByteBuf buf) {
        return new AbilityC2SPacket(buf.readInt());
    }


    static {
        CODEC = PacketCodec.of(AbilityC2SPacket::write, AbilityC2SPacket::read);
    }
}
