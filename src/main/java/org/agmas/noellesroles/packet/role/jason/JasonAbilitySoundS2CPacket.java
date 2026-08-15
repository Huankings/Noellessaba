package org.agmas.noellesroles.packet.role.jason;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.registry.NoellesRolesCore;

/**
 * 杰森“无恶不在”客户端音效控制包。
 *
 * <p>持续音 {@code jason_ability_last} 需要在主动解除时立刻停止。
 * 原版一次性播放包只能“播一个声音”，不能稳定控制循环声源的生命周期，
 * 所以这里用一个很小的 S2C 包把“开始循环 / 停止循环 / 播放指定一次性音效”交给客户端本地声音控制器。</p>
 */
public record JasonAbilitySoundS2CPacket(Action action) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = Identifier.of(NoellesRolesCore.MOD_ID, "jason_ability_sound");
    public static final Id<JasonAbilitySoundS2CPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, JasonAbilitySoundS2CPacket> CODEC =
            PacketCodec.of(JasonAbilitySoundS2CPacket::write, JasonAbilitySoundS2CPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(this.action);
    }

    public static JasonAbilitySoundS2CPacket read(PacketByteBuf buf) {
        return new JasonAbilitySoundS2CPacket(buf.readEnumConstant(Action.class));
    }

    public enum Action {
        /**
         * 播放发动瞬间的全局音效。
         */
        PLAY_START,
        /**
         * 开始或刷新持续循环音效。
         */
        START_LOOP,
        /**
         * 立刻停止持续循环音效。
         */
        STOP_LOOP,
        /**
         * 播放主动解除时只有杰森和非存活玩家能听到的结束音效。
         */
        PLAY_END,
        /**
         * 播放惊吓音效，只发给杰森和被惊吓玩家本人。
         */
        PLAY_JUMP_SCARE
    }
}
