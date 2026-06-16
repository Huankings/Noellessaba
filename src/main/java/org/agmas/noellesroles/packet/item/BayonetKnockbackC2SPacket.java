package org.agmas.noellesroles.packet.item;

import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.assassin.BayonetKnockbackHandler;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.jetbrains.annotations.NotNull;

/**
 * 刺刀左键击退数据包。
 *
 * <p>它的存在是为了绕开“真实玩家与 carpet 假人在原版 attack 流水线里表现不一致”的问题：
 * 客户端一旦确认自己正拿着刺刀左键命中了玩家，就直接告诉服务端施加专属击退。</p>
 */
public record BayonetKnockbackC2SPacket(int target) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "bayonet_knockback");
    public static final Id<BayonetKnockbackC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, BayonetKnockbackC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public BayonetKnockbackC2SPacket decode(RegistryByteBuf buf) {
            return new BayonetKnockbackC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, BayonetKnockbackC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<BayonetKnockbackC2SPacket> {
        @Override
        public void receive(@NotNull BayonetKnockbackC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity attacker = context.player();
            net.minecraft.entity.Entity rawTarget = attacker.getServerWorld().getEntityById(payload.target());
            if (rawTarget == null) {
                return;
            }

            /*
             * 刺刀左键击退客户端本身也会在命中播放体时发包，
             * 所以这里要先把“播放体强制结束”这条特殊分支拦在前面。
             *
             * 这样玩家看到准星已经锁到皮套时，左键击退也会像其它玩法武器一样正确结束播放。
             */
            MagicianServerHooks.recordBayonetKnockback(attacker);
            if (MagicianServerHooks.stopPlaybackByWeaponTarget(
                    rawTarget,
                    attacker,
                    dev.doctor4t.wathe.game.GameConstants.DeathReasons.KNIFE,
                    MagicianServerHooks.getWeaponName(attacker.getMainHandStack())
            )) {
                return;
            }

            if (!(rawTarget instanceof PlayerEntity target)) {
                return;
            }
            if (!BayonetKnockbackHandler.canKnockback(attacker, target)) {
                return;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(target) || target.distanceTo(attacker) > 3.0F) {
                return;
            }

            BayonetKnockbackHandler.applyKnockback(attacker, target);

            /*
             * 左键击退本身没有冷却，但需要把挥手动画广播给周围玩家，
             * 否则只有本地客户端能看见自己挥刀，其他人会感觉动作不同步。
             */
            attacker.swingHand(Hand.MAIN_HAND, true);
        }
    }
}
