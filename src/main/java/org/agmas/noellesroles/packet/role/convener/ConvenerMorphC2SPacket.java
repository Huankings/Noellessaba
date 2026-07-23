package org.agmas.noellesroles.packet.role.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.convener.ConvenerDisguiseComponent;
import org.agmas.noellesroles.roles.convener.ConvenerPlayerComponent;

import java.util.UUID;

/**
 * 召集者背包头像选择包。
 */
public record ConvenerMorphC2SPacket(UUID targetUuid) implements CustomPayload {
    public static final Identifier PAYLOAD_ID = Identifier.of(Noellesroles.MOD_ID, "convener_morph");
    public static final Id<ConvenerMorphC2SPacket> ID = new Id<>(PAYLOAD_ID);
    public static final PacketCodec<RegistryByteBuf, ConvenerMorphC2SPacket> CODEC =
            PacketCodec.of(ConvenerMorphC2SPacket::write, ConvenerMorphC2SPacket::read);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private void write(PacketByteBuf buf) {
        buf.writeUuid(this.targetUuid);
    }

    private static ConvenerMorphC2SPacket read(PacketByteBuf buf) {
        return new ConvenerMorphC2SPacket(buf.readUuid());
    }

    public static void handle(ConvenerMorphC2SPacket payload, ServerPlayerEntity player) {
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        if (!gameWorld.isRole(player, Noellesroles.CONVENER)) {
            return;
        }

        ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(player);
        if (payload.targetUuid().equals(player.getUuid())) {
            disguise.clearDisguise();
            return;
        }

        ConvenerPlayerComponent convener = ConvenerPlayerComponent.KEY.get(player);
        if (!convener.knowsDisguise(payload.targetUuid())) {
            return;
        }
        disguise.setPersistentDisguise(payload.targetUuid());
    }
}
