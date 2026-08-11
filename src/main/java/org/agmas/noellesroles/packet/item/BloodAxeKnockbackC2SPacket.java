package org.agmas.noellesroles.packet.item;

import dev.doctor4t.wathe.api.visibility.TargetVisibilityApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.agmas.noellesroles.ModItems;
import org.agmas.noellesroles.registry.NoellesRolesCore;
import org.agmas.noellesroles.roles.magician.MagicianServerHooks;
import org.agmas.noellesroles.roles.spring_trap.SpringTrapConstants;
import org.jetbrains.annotations.NotNull;

/**
 * 血斧左键击退。
 *
 * <p>这个包只做位移，不造成伤害也不写冷却；服务端仍会检查目标是否合法。</p>
 */
public record BloodAxeKnockbackC2SPacket(int target) implements CustomPayload {
    public static final Identifier PACKET_ID = Identifier.of(NoellesRolesCore.MOD_ID, "blood_axe_knockback");
    public static final Id<BloodAxeKnockbackC2SPacket> ID = new Id<>(PACKET_ID);
    public static final PacketCodec<RegistryByteBuf, BloodAxeKnockbackC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public BloodAxeKnockbackC2SPacket decode(RegistryByteBuf buf) {
            return new BloodAxeKnockbackC2SPacket(buf.readInt());
        }

        @Override
        public void encode(RegistryByteBuf buf, BloodAxeKnockbackC2SPacket value) {
            buf.writeInt(value.target());
        }
    };

    @Override
    public @NotNull Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static final class Receiver implements ServerPlayNetworking.PlayPayloadHandler<BloodAxeKnockbackC2SPacket> {
        @Override
        public void receive(@NotNull BloodAxeKnockbackC2SPacket payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity attacker = context.player();
            var rawTarget = attacker.getServerWorld().getEntityById(payload.target());
            if (!attacker.getMainHandStack().isOf(ModItems.BLOOD_AXE)) {
                return;
            }

            if (MagicianServerHooks.stopPlaybackByWeaponTarget(
                    rawTarget,
                    attacker,
                    dev.doctor4t.wathe.game.GameConstants.DeathReasons.KNIFE,
                    MagicianServerHooks.getWeaponName(attacker.getMainHandStack())
            )) {
                attacker.swingHand(Hand.MAIN_HAND, true);
                return;
            }

            if (!(rawTarget instanceof PlayerEntity target)
                    || !GameFunctions.isPlayerAliveAndSurvival(attacker)
                    || !GameFunctions.isPlayerAliveAndSurvival(target)
                    || target.distanceTo(attacker) > SpringTrapConstants.BLOOD_AXE_TARGET_RANGE
                    || !TargetVisibilityApi.canAttackPlayer(attacker, target)) {
                return;
            }

            applyKnockback(attacker, target);
            attacker.swingHand(Hand.MAIN_HAND, true);
        }
    }

    private static void applyKnockback(@NotNull PlayerEntity attacker, @NotNull PlayerEntity target) {
        target.setAttacker(attacker);

        double x = attacker.getX() - target.getX();
        double z = attacker.getZ() - target.getZ();
        if (x * x + z * z < 1.0E-4D) {
            x = (attacker.getRandom().nextDouble() - 0.5D) * 0.01D;
            z = (attacker.getRandom().nextDouble() - 0.5D) * 0.01D;
        }

        Vec3d pushDirection = new Vec3d(x, 0.0D, z).normalize().multiply(SpringTrapConstants.BLOOD_AXE_KNOCKBACK_STRENGTH);
        Vec3d currentVelocity = target.getVelocity();
        target.setVelocity(
                currentVelocity.x * 0.5D - pushDirection.x,
                target.isOnGround() ? Math.min(0.4D, currentVelocity.y * 0.5D + SpringTrapConstants.BLOOD_AXE_KNOCKBACK_UPWARD) : currentVelocity.y,
                currentVelocity.z * 0.5D - pushDirection.z
        );
        target.velocityModified = true;
        if (target instanceof ServerPlayerEntity serverTarget) {
            serverTarget.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverTarget));
        }
    }
}
