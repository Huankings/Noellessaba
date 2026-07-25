package org.agmas.noellesroles.packet.modifiers.dual_personality;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 双重人格主动切换请求。
 *
 * <p>客户端只表达“我按下了切换键”，真正是否处于活跃人格、是否在普通轮换阶段，
 * 都由服务端 DualPersonalityManager 二次校验。</p>
 */
public record DualPersonalitySwitchC2SPacket() implements CustomPayload {
    public static final Identifier PAYLOAD_ID = Identifier.of(NoellesRolesCore.MOD_ID, "dual_personality_switch");
    public static final Id<DualPersonalitySwitchC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, DualPersonalitySwitchC2SPacket> CODEC =
            PacketCodec.of((value, buf) -> {
            }, buf -> new DualPersonalitySwitchC2SPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
