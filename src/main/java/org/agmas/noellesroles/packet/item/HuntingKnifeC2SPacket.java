package org.agmas.noellesroles.packet.item;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.hunter.HunterConstants;
import org.agmas.noellesroles.roles.hunter.HunterPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 猎刀命中数据包。
 *
 * <p>客户端只发送“松手时准星命中的目标 id”；距离、存活、手持物和蓄力窗口全部在服务端重新确认。</p>
 */
public record HuntingKnifeC2SPacket(int target) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(Noellesroles.MOD_ID, "hunting_knife");
    public static final Id<HuntingKnifeC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, HuntingKnifeC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public HuntingKnifeC2SPacket decode(RegistryByteBuf buf) {
            return new HuntingKnifeC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, HuntingKnifeC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<HuntingKnifeC2SPacket> {
        @Override
        public void receive(@NotNull HuntingKnifeC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            HunterPlayerComponent hunter = HunterPlayerComponent.KEY.get(player);

            /*
             * 不能直接检查 ItemCooldownManager 是否冷却中：
             * 服务端 onStoppedUsing 可能已经先写入“疾跑举刀临时冷却”，随后客户端命中包才到。
             * 所以这里改用组件里的释放窗口判断，既不误挡正常猎刀命中，也不会接受凭空伪造的延迟发包。
             */
            if (!player.getMainHandStack().isOf(ModItems.HUNTING_KNIFE) || !hunter.canAcceptReleasedHit()) {
                return;
            }
            if (!(player.getServerWorld().getEntityById(payload.target()) instanceof PlayerEntity target)
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.distanceTo(player) > HunterConstants.HUNTING_KNIFE_TARGET_RANGE) {
                return;
            }

            if (target instanceof ServerPlayerEntity serverTarget) {
                ItemStack replayStack = player.getMainHandStack().isOf(ModItems.HUNTING_KNIFE)
                        ? player.getMainHandStack()
                        : new ItemStack(ModItems.HUNTING_KNIFE);
                /*
                 * 死因沿用 Wathe 原版 knife，保持与 kinssaba 一致；
                 * 但回放记录里保存真实 ItemStack，使展示名可以显示为“猎刀”。
                 */
                GameRecordManager.recordItemHit(player, replayStack, GameConstants.DeathReasons.KNIFE, serverTarget, null);
            }

            hunter.reset();
            /*
             * 命中后的 45 秒冷却只写给正式存活玩家。
             * 创造/旁观语义玩家主要用于本地或服主调试，按用户要求不让冷却影响连续测试。
             */
            if (!GameFunctions.isPlayerSpectatingOrCreative(player)) {
                player.getItemCooldownManager().set(ModItems.HUNTING_KNIFE, HunterConstants.HUNTING_KNIFE_COOLDOWN_TICKS);
            }
            GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);
            target.playSound(WatheSounds.ITEM_KNIFE_STAB, 1.0F, 1.0F);
            player.swingHand(Hand.MAIN_HAND, true);
        }
    }
}
