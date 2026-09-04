package org.agmas.noellesroles.packet.item;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.dreamer.DreamerConstants;
import org.agmas.noellesroles.roles.dreamer.DreamerKillerComponent;
import org.agmas.noellesroles.framing.DelusionPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 幻觉注剂命中数据包。
 *
 * <p>客户端只发送准心命中的实体 id；服务端重新检查手持物、冷却、目标存活、
 * 距离和 TargetVisibilityApi，避免伪造包直接给任意玩家写入幻觉状态。</p>
 */
public record DelusionSyringeC2SPacket(int target) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(NoellesRolesCore.MOD_ID, "delusion_syringe");
    public static final Id<DelusionSyringeC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, DelusionSyringeC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public DelusionSyringeC2SPacket decode(RegistryByteBuf buf) {
            return new DelusionSyringeC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, DelusionSyringeC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final class Receiver implements ServerPlayNetworking.PlayPayloadHandler<DelusionSyringeC2SPacket> {
        @Override
        public void receive(
                @NotNull DelusionSyringeC2SPacket payload,
                ServerPlayNetworking.@NotNull Context context
        ) {
            ServerPlayerEntity player = context.player();
            boolean ignoresCooldown = GameFunctions.isPlayerSpectatingOrCreative(player);
            ItemStack stack = player.getMainHandStack();

            if (!stack.isOf(ModItems.DELUSION_SYRINGE)
                    || (!ignoresCooldown
                    && player.getItemCooldownManager().isCoolingDown(ModItems.DELUSION_SYRINGE))) {
                return;
            }

            /*
             * 使用者可以是旁观/创造调试身份，但被注射者必须始终是正常存活玩家；
             * 这样调试绕过只影响注射者冷却，不会把死者或旁观者变成幻觉目标。
             */
            if (!(player.getServerWorld().getEntityById(payload.target()) instanceof ServerPlayerEntity target)
                    || target == player
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.distanceTo(player) > DreamerConstants.DELUSION_SYRINGE_TARGET_RANGE
                    || !TargetVisibilityApi.canAttackPlayer(player, target)) {
                return;
            }

            DelusionPlayerComponent.KEY.get(target).startDelusion(target, player.getUuid());

            /*
             * 回放额外保存完整物品数据，通用 formatter 才能显示本地化的白色 [物品名]。
             * 未来其它针剂可以复用同样的 recordItemUse + formatter 结构。
             */
            GameRecordManager.recordItemUse(
                    player,
                    Registries.ITEM.getId(stack.getItem()),
                    target,
                    dev.doctor4t.wathe.game.GameFunctions.createReplayItemData(player.getServerWorld(), stack)
            );

            if (!ignoresCooldown) {
                /*
                 * 只有正式成功注射才进入 45 秒冷却。
                 * 直接覆盖为普通使用冷却；Wathe tooltip API 会读取这次新写入的真实时长。
                 */
                player.getItemCooldownManager().set(
                        ModItems.DELUSION_SYRINGE,
                        DreamerConstants.DELUSION_SYRINGE_COOLDOWN_TICKS
                );
            }

            // 只向注射者本人播放，不在世界坐标播放，目标和附近玩家都听不到。
            player.playSoundToPlayer(
                    NoellesRolesSounds.ITEM_SYRINGE_STAB,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
            player.swingHand(Hand.MAIN_HAND, true);
        }
    }
}
