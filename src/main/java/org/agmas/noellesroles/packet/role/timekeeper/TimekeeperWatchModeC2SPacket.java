package org.agmas.noellesroles.packet.role.timekeeper;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 怀表模式切换 C2S 包。
 *
 * <p>客户端左键拦截只负责即时反馈；服务端收到这个包后仍会重新检查职业、存活状态和主手物品，
 * 然后把模式写进物品数据组件，确保最终状态以服务端为准。</p>
 */
public record TimekeeperWatchModeC2SPacket(int modeOrdinal) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = Identifier.of(NoellesRolesCore.MOD_ID, "timekeeper_watch_mode");
    public static final Id<TimekeeperWatchModeC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, TimekeeperWatchModeC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            TimekeeperWatchModeC2SPacket::modeOrdinal,
            TimekeeperWatchModeC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
