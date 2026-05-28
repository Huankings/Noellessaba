package org.agmas.noellesroles.client.roles.coward;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.roles.angel.AngelPlayerComponent;
import org.agmas.noellesroles.roles.coward.CowardConstants;
import org.agmas.noellesroles.roles.coward.CowardPlayerComponent;
import org.agmas.noellesroles.roles.coward.CowardThreatSnapshot;
import org.agmas.noellesroles.roles.coward.SedativePlayerComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 胆小鬼客户端表现与左轮偏移入口。
 */
public final class CowardClientEffects {
    private static int ticksSinceLastPulse = 0;
    private static float pulseProgress = 0.0f;
    private static float currentNetPulse = 0.0f;
    private static boolean pulsing = false;
    private static boolean dangerActiveLastTick = false;

    private CowardClientEffects() {
    }

    public static void tick(MinecraftClient client) {
        if (!shouldApplySense(client.player)) {
            resetSense();
            return;
        }

        ClientPlayerEntity player = client.player;
        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        CowardThreatSnapshot snapshot = CowardThreatSnapshot.collect(player, gameWorld);
        if (!snapshot.hasEffectiveDanger()) {
            resetSense();
            return;
        }

        currentNetPulse = MathHelper.clamp(snapshot.netPulse(), 0.0f, 1.5f);
        int currentIntervalTicks = snapshot.pulseIntervalTicks();
        if (!dangerActiveLastTick) {
            triggerPulse(player);
            ticksSinceLastPulse = 0;
            dangerActiveLastTick = true;
            return;
        }
        dangerActiveLastTick = true;

        ticksSinceLastPulse++;
        if (ticksSinceLastPulse >= currentIntervalTicks) {
            triggerPulse(player);
            ticksSinceLastPulse = 0;
        }
    }

    public static void reset() {
        resetSense();
    }

    public static float getFovPulseMultiplier(float tickDelta) {
        if (!pulsing) {
            return 1.0f;
        }

        pulseProgress += tickDelta * CowardConstants.FOV_PULSE_SPEED;
        if (pulseProgress >= 1.0f) {
            pulsing = false;
            pulseProgress = 0.0f;
            return 1.0f;
        }

        float normalizedPulse = MathHelper.clamp(currentNetPulse, 0.0f, 1.0f);
        float amplitude = MathHelper.lerp(normalizedPulse, CowardConstants.FOV_PULSE_MIN, CowardConstants.FOV_PULSE_MAX);
        return getPulseMultiplier(amplitude);
    }

    public static boolean shouldApplyRevolverShake(@Nullable PlayerEntity player) {
        if (!(player instanceof ClientPlayerEntity clientPlayer)) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != clientPlayer) {
            return false;
        }
        if (!clientPlayer.getMainHandStack().isOf(WatheItems.REVOLVER)) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(clientPlayer.getWorld());
        return gameWorld.isRole(clientPlayer, Noellesroles.COWARD)
                && gameWorld.isRunning()
                && GameFunctions.isPlayerAliveAndSurvival(clientPlayer)
                && !isSuppressed(clientPlayer);
    }

    public static @NotNull CowardRevolverOffset getRevolverOffset(@Nullable PlayerEntity player) {
        if (!shouldApplyRevolverShake(player)) {
            return CowardRevolverOffset.ZERO;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(player.getWorld());
        CowardThreatSnapshot snapshot = CowardThreatSnapshot.collect(player, gameWorld);
        float strength = snapshot.revolverShakeStrength();
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
        double time = (player.age + tickDelta) * CowardConstants.REVOLVER_SHAKE_TIME_SCALE;

        float yawOffset = (float) (
                Math.sin(time * 2.1)
                        + Math.sin(time * 4.0 + 0.9) * 0.38
        ) * CowardConstants.REVOLVER_YAW_SHAKE_DEGREES * strength;
        float pitchOffset = (float) (
                Math.cos(time * 2.5 + 0.4)
                        + Math.sin(time * 3.3 + 1.7) * 0.28
        ) * CowardConstants.REVOLVER_PITCH_SHAKE_DEGREES * strength;

        double sideOffset = Math.sin(time * 2.2 + 0.25) * CowardConstants.REVOLVER_POSITION_SHAKE * strength;
        double verticalOffset = Math.cos(time * 2.9 + 1.1) * CowardConstants.REVOLVER_POSITION_VERTICAL_SHAKE * strength;
        double depthOffset = Math.sin(time * 1.8 + 2.3) * CowardConstants.REVOLVER_POSITION_DEPTH_SHAKE * strength;

        float adjustedYaw = player.getYaw() + yawOffset;
        float adjustedPitch = player.getPitch() + pitchOffset;
        Vec3d right = Vec3d.fromPolar(0.0f, adjustedYaw + 90.0f).normalize();
        Vec3d forward = Vec3d.fromPolar(adjustedPitch, adjustedYaw).normalize();
        Vec3d positionOffset = right.multiply(sideOffset)
                .add(0.0, verticalOffset, 0.0)
                .add(forward.multiply(depthOffset));

        return new CowardRevolverOffset(yawOffset, pitchOffset, positionOffset);
    }

    public static @Nullable HitResult getOffsetRevolverTarget(@NotNull PlayerEntity user) {
        if (!shouldApplyRevolverShake(user)) {
            return null;
        }

        CowardRevolverOffset offset = getRevolverOffset(user);
        float adjustedYaw = user.getYaw() + offset.yawDegrees();
        float adjustedPitch = user.getPitch() + offset.pitchDegrees();

        Vec3d start = user.getEyePos().add(offset.positionOffset());
        Vec3d direction = Vec3d.fromPolar(adjustedPitch, adjustedYaw).normalize();
        Vec3d end = start.add(direction.multiply(CowardConstants.REVOLVER_RAYCAST_DISTANCE));

        HitResult blockHit = user.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                user
        ));

        double maxDistanceSquared = start.squaredDistanceTo(end);
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistanceSquared = start.squaredDistanceTo(blockHit.getPos());
        }

        Box searchBox = user.getBoundingBox()
                .stretch(direction.multiply(CowardConstants.REVOLVER_RAYCAST_DISTANCE))
                .expand(1.0);
        EntityHitResult entityHit = ProjectileUtil.raycast(
                user,
                start,
                end,
                searchBox,
                entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player),
                maxDistanceSquared
        );
        return entityHit != null ? entityHit : blockHit;
    }

    private static boolean shouldApplySense(@Nullable PlayerEntity player) {
        if (!(player instanceof ClientPlayerEntity clientPlayer)) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != clientPlayer) {
            return false;
        }

        GameWorldComponent gameWorld = GameWorldComponent.KEY.get(clientPlayer.getWorld());
        return gameWorld.isRole(clientPlayer, Noellesroles.COWARD)
                && gameWorld.isRunning()
                && GameFunctions.isPlayerAliveAndSurvival(clientPlayer)
                && CowardPlayerComponent.KEY.get(clientPlayer).isSenseUnlocked()
                && !isSuppressed(clientPlayer);
    }

    private static boolean isSuppressed(@NotNull PlayerEntity player) {
        return SedativePlayerComponent.KEY.get(player).isActive()
                || AngelPlayerComponent.KEY.get(player).isSoothed();
    }

    private static void triggerPulse(@NotNull ClientPlayerEntity player) {
        pulsing = true;
        pulseProgress = 0.0f;

        float normalizedPulse = MathHelper.clamp(currentNetPulse, 0.0f, 1.0f);
        float volume = MathHelper.lerp(normalizedPulse, CowardConstants.HEARTBEAT_MIN_VOLUME, CowardConstants.HEARTBEAT_MAX_VOLUME);
        player.playSoundToPlayer(
                SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.PLAYERS,
                volume,
                CowardConstants.HEARTBEAT_PITCH
        );
    }

    private static float getPulseMultiplier(float amplitude) {
        if (pulseProgress <= CowardConstants.FOV_PULSE_PRIMARY_END) {
            float phase = pulseProgress / CowardConstants.FOV_PULSE_PRIMARY_END;
            return 1.0f - amplitude * (float) Math.sin(Math.PI * phase);
        }
        if (pulseProgress >= CowardConstants.FOV_PULSE_SECONDARY_START
                && pulseProgress <= CowardConstants.FOV_PULSE_SECONDARY_END) {
            float phase = (pulseProgress - CowardConstants.FOV_PULSE_SECONDARY_START)
                    / (CowardConstants.FOV_PULSE_SECONDARY_END - CowardConstants.FOV_PULSE_SECONDARY_START);
            return 1.0f - amplitude * 0.55f * (float) Math.sin(Math.PI * phase);
        }
        return 1.0f;
    }

    private static void resetSense() {
        ticksSinceLastPulse = 0;
        pulseProgress = 0.0f;
        currentNetPulse = 0.0f;
        pulsing = false;
        dangerActiveLastTick = false;
    }

    public record CowardRevolverOffset(float yawDegrees, float pitchDegrees, Vec3d positionOffset) {
        public static final CowardRevolverOffset ZERO = new CowardRevolverOffset(0.0f, 0.0f, Vec3d.ZERO);

        public boolean isZero() {
            return this.yawDegrees == 0.0f
                    && this.pitchDegrees == 0.0f
                    && this.positionOffset.equals(Vec3d.ZERO);
        }
    }
}
