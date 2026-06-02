package org.agmas.noellesroles.packet.item;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;

/**
 * 刺刀瞬杀数据包。
 *
 * <p>客户端只负责告诉服务端“我瞄准了哪个玩家”，
 * 其余距离校验、命中记录、真正击杀与冷却结算都在服务端统一处理。</p>
 */
public record BayonetStabC2SPacket(int target) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "bayonet_stab");
    public static final Id<BayonetStabC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, BayonetStabC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public BayonetStabC2SPacket decode(RegistryByteBuf buf) {
            return new BayonetStabC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, BayonetStabC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<BayonetStabC2SPacket> {
        @Override
        public void receive(@NotNull BayonetStabC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            if (!player.getMainHandStack().isOf(ModItems.BAYONET)) {
                return;
            }
            if (player.getItemCooldownManager().isCoolingDown(ModItems.BAYONET)) {
                return;
            }
            if (!(player.getServerWorld().getEntityById(payload.target()) instanceof PlayerEntity target)
                    || !GameFunctions.isPlayerAliveAndSurvival(target)) {
                return;
            }
            if (target.distanceTo(player) > 3.0F) {
                return;
            }

            if (target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemHit(
                        player,
                        player.getMainHandStack(),
                        GameConstants.DeathReasons.KNIFE,
                        serverTarget,
                        null
                );
            }

            /*
             * 右键刺刀的本地挥手动画目前主要由客户端先行播放，
             * 但冒险模式右键对准玩家时，不一定会再走到原版那条能自动广播手臂动画的服务端交互链。
             * 结果就是：
             * 1. 自己看得到挥刀；
             * 2. 旁观者却可能完全看不到。
             *
             * 因此这里在“服务端确认本次刺杀命中了有效目标”后，显式广播一次主手挥动。
             * 这样无论创造还是冒险，只要刺刀真正生效，周围玩家都能稳定看到动作。
             */
            player.swingHand(Hand.MAIN_HAND, true);
            GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);

            if (!player.isCreative()) {
                player.getItemCooldownManager().set(
                        ModItems.BAYONET,
                        GameConstants.ITEM_COOLDOWNS.getOrDefault(ModItems.BAYONET, 0)
                );
            }
        }
    }
}
