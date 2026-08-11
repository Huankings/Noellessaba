package org.agmas.noellesroles.packet.item;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesDeathReasons;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapPlayerComponent;
import org.jetbrains.annotations.NotNull;

/**
 * 血斧右键蓄力刺杀。
 *
 * <p>客户端只负责发送准星命中的玩家 id；服务端重新校验所有条件，避免伪造包绕过冷却、距离和隐藏目标规则。</p>
 */
public record BloodAxeStabC2SPacket(int target) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(NoellesRolesCore.MOD_ID, "blood_axe_stab");
    public static final Id<BloodAxeStabC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, BloodAxeStabC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public BloodAxeStabC2SPacket decode(RegistryByteBuf buf) {
            return new BloodAxeStabC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, BloodAxeStabC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final class Receiver implements ServerPlayNetworking.PlayPayloadHandler<BloodAxeStabC2SPacket> {
        @Override
        public void receive(@NotNull BloodAxeStabC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            ItemStack stack = player.getMainHandStack();
            if (!stack.isOf(ModItems.BLOOD_AXE) || player.getItemCooldownManager().isCoolingDown(ModItems.BLOOD_AXE)) {
                return;
            }

            var rawTarget = player.getServerWorld().getEntityById(payload.target());
            if (MagicianServerHooks.stopPlaybackByWeaponTarget(
                    rawTarget,
                    player,
                    NoellesDeathReasons.DEATH_REASON_AXE,
                    MagicianServerHooks.getWeaponName(stack)
            )) {
                player.swingHand(Hand.MAIN_HAND, true);
                SpringTrapPlayerComponent.KEY.get(player).clearBloodAxeStartCooldown();
                player.getItemCooldownManager().set(ModItems.BLOOD_AXE, SpringTrapConstants.BLOOD_AXE_COOLDOWN_TICKS);
                return;
            }

            if (!(rawTarget instanceof PlayerEntity target)
                    || !GameFunctions.isPlayerAliveAndSurvival(player)
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.distanceTo(player) > SpringTrapConstants.BLOOD_AXE_TARGET_RANGE
                    || !TargetVisibilityApi.canAttackPlayer(player, target)) {
                return;
            }

            if (target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemHit(player, stack, NoellesDeathReasons.DEATH_REASON_AXE, serverTarget, null);
            }

            NbtCompound replayData = GameFunctions.createReplayItemData(player.getServerWorld(), stack);
            GameFunctions.killPlayer(target, true, player, NoellesDeathReasons.DEATH_REASON_AXE, replayData);
            player.getWorld().playSound(null, target.getX(), target.getEyeY(), target.getZ(), WatheSounds.ITEM_KNIFE_STAB, SoundCategory.PLAYERS, 1.0F, 1.0F);
            player.swingHand(Hand.MAIN_HAND, true);
            if (!player.isCreative()) {
                SpringTrapPlayerComponent.KEY.get(player).clearBloodAxeStartCooldown();
                player.getItemCooldownManager().set(ModItems.BLOOD_AXE, SpringTrapConstants.BLOOD_AXE_COOLDOWN_TICKS);
            }
        }
    }
}
