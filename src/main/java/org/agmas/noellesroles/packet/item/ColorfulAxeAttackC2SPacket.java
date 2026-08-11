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
import org.jetbrains.annotations.NotNull;

/**
 * 彩虹斧无攻速左键击杀。
 *
 * <p>Wathe 原生疯魔近战会检查攻击冷却；彩虹斧需要允许连续点击，所以这里单独走 C2S 包和服务端验证。</p>
 */
public record ColorfulAxeAttackC2SPacket(int target) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(NoellesRolesCore.MOD_ID, "colorful_axe_attack");
    public static final Id<ColorfulAxeAttackC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, ColorfulAxeAttackC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public ColorfulAxeAttackC2SPacket decode(RegistryByteBuf buf) {
            return new ColorfulAxeAttackC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, ColorfulAxeAttackC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final class Receiver implements ServerPlayNetworking.PlayPayloadHandler<ColorfulAxeAttackC2SPacket> {
        @Override
        public void receive(@NotNull ColorfulAxeAttackC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            ItemStack stack = player.getMainHandStack();
            if (!stack.isOf(ModItems.COLORFUL_AXE)) {
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
                return;
            }

            if (!(rawTarget instanceof PlayerEntity target)
                    || !GameFunctions.isPlayerAliveAndSurvival(player)
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.distanceTo(player) > SpringTrapConstants.COLORFUL_AXE_TARGET_RANGE
                    || !TargetVisibilityApi.canAttackPlayer(player, target)) {
                return;
            }

            if (target instanceof ServerPlayerEntity serverTarget) {
                GameRecordManager.recordItemHit(player, stack, NoellesDeathReasons.DEATH_REASON_AXE, serverTarget, null);
            }

            NbtCompound replayData = GameFunctions.createReplayItemData(player.getServerWorld(), stack);
            GameFunctions.killPlayer(target, true, player, NoellesDeathReasons.DEATH_REASON_AXE, replayData);
            player.getWorld().playSound(null, target.getX(), target.getEyeY(), target.getZ(), WatheSounds.ITEM_BAT_HIT, SoundCategory.PLAYERS, 3.0F, 1.0F);
            player.swingHand(Hand.MAIN_HAND, true);
        }
    }
}
