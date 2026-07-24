package org.agmas.noellesroles.packet.item;

import org.agmas.noellesroles.registry.NoellesEventIds;
import org.agmas.noellesroles.registry.NoellesRolesCore;

import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.roles.cook.CookConstants;
import org.agmas.noellesroles.roles.engineer.StunnedPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 平底锅命中数据包。
 */
public record PanC2SPacket(int target) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(NoellesRolesCore.MOD_ID, "pan");
    public static final Id<PanC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, PanC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public PanC2SPacket decode(RegistryByteBuf buf) {
            return new PanC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, PanC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<PanC2SPacket> {
        @Override
        public void receive(@NotNull PanC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
            if (!player.getMainHandStack().isOf(ModItems.PAN)
                    || (!ignoresCooldown && player.getItemCooldownManager().isCoolingDown(ModItems.PAN))) {
                return;
            }
            if (!(player.getServerWorld().getEntityById(payload.target()) instanceof PlayerEntity target)
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.distanceTo(player) > CookConstants.PAN_TARGET_RANGE) {
                return;
            }

            /*
             * 眩晕使用 NoellesRoles 已有的 stunned 组件，避免再造一套输入锁状态。
             * 这里传入 pan_stun_end，让组件自然结束时能写平底锅自己的回放事件。
             */
            StunnedPlayerComponent.KEY.get(target).stun(CookConstants.PAN_STUN_TICKS, NoellesEventIds.PAN_STUN_END_EVENT);

            if (target instanceof ServerPlayerEntity serverTarget) {
                ItemStack replayStack = player.getMainHandStack().isOf(ModItems.PAN)
                        ? player.getMainHandStack()
                        : new ItemStack(ModItems.PAN);
                GameRecordManager.recordItemHit(player, replayStack, serverTarget, null);
            }

            /*
             * 平底锅命中后的冷却只约束正式对局里的存活玩家。
             * 旁观/创造玩家常用于测试命中、眩晕和回放，不写冷却方便连续调试。
             */
            if (!ignoresCooldown) {
                player.getItemCooldownManager().set(ModItems.PAN, CookConstants.PAN_COOLDOWN_TICKS);
            }
            target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8F, 0.8F);
            player.swingHand(Hand.MAIN_HAND, true);
        }
    }
}
