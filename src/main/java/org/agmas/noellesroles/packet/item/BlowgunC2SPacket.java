package org.agmas.noellesroles.packet.item;

import org.agmas.noellesroles.registry.NoellesRoleRegistry;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.drugmaker.DrugmakerConstants;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * 吹矢命中数据包。
 *
 * <p>客户端只告诉服务端“准星命中了哪个实体 id”，服务端仍会重新校验目标、距离和机器人免疫。</p>
 */
public record BlowgunC2SPacket(int target) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(NoellesRolesCore.MOD_ID, "blowgun");
    public static final Id<BlowgunC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, BlowgunC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public BlowgunC2SPacket decode(RegistryByteBuf buf) {
            return new BlowgunC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, BlowgunC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<BlowgunC2SPacket> {
        @Override
        public void receive(@NotNull BlowgunC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            if (!(player.getServerWorld().getEntityById(payload.target()) instanceof PlayerEntity target)
                    || target.distanceTo(player) > DrugmakerConstants.BLOWGUN_TARGET_RANGE) {
                return;
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
            PlayerPoisonComponent targetPoison = PlayerPoisonComponent.KEY.get(target);
            ItemStack replayStack = player.getMainHandStack().isOf(ModItems.BLOWGUN)
                    ? player.getMainHandStack()
                    : new ItemStack(ModItems.BLOWGUN);

            if (gameWorld.isRole(target, NoellesRoleRegistry.ROBOT)) {
                if (target instanceof ServerPlayerEntity serverTarget) {
                    NbtCompound extra = new NbtCompound();
                    extra.putBoolean("robot_failed", true);
                    GameRecordManager.recordItemHit(player, replayStack, serverTarget, extra);
                }
                player.sendMessage(Text.translatable("tip.noellesroles.drugmaker.poison_failed").withColor(Color.RED.getRGB()), true);
                return;
            }

            if (target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemHit(player, replayStack, serverTarget, null);
            }
            NbtCompound poisonData = GameFunctions.createReplayItemData(player.getServerWorld(), replayStack);
            if (targetPoison.poisonTicks > 0) {
                int reduction = DrugmakerConstants.BLOWGUN_POISON_REDUCTION_MIN_TICKS
                        + player.getRandom().nextInt(DrugmakerConstants.BLOWGUN_POISON_REDUCTION_RANDOM_BOUND);
                int poisonTicks = Math.max(0, targetPoison.poisonTicks - reduction);
                targetPoison.setDetailedPoisonTicks(poisonTicks, player.getUuid(), GameConstants.DeathReasons.POISON, poisonData);
                return;
            }

            int poisonTicks = PlayerPoisonComponent.clampTime.getLeft()
                    + player.getRandom().nextInt(PlayerPoisonComponent.clampTime.getRight() - PlayerPoisonComponent.clampTime.getLeft());
            targetPoison.setDetailedPoisonTicks(poisonTicks, player.getUuid(), GameConstants.DeathReasons.POISON, poisonData);
        }
    }
}
