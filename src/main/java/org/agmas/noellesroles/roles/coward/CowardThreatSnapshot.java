package org.agmas.noellesroles.roles.coward;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * 胆小鬼附近威胁快照。
 *
 * <p>服务端与客户端共用同一套距离分层计算，保证 san、FOV、心跳和左轮抖动的基础强度一致。</p>
 */
public record CowardThreatSnapshot(
        float dangerPulse,
        float calmPulse,
        float dangerSan,
        float calmSan
) {

    public static final CowardThreatSnapshot EMPTY = new CowardThreatSnapshot(0.0f, 0.0f, 0.0f, 0.0f);

    public static CowardThreatSnapshot collect(PlayerEntity player, GameWorldComponent gameWorld) {
        if (player == null || gameWorld == null) {
            return EMPTY;
        }

        float dangerPulse = 0.0f;
        float calmPulse = 0.0f;
        float dangerSan = 0.0f;
        float calmSan = 0.0f;

        for (PlayerEntity other : player.getWorld().getPlayers()) {
            if (other == null || other.getUuid().equals(player.getUuid())) {
                continue;
            }
            if (!GameFunctions.isPlayerAliveAndSurvival(other)) {
                continue;
            }

            double distance = player.distanceTo(other);
            if (distance > CowardConstants.SENSE_RADIUS) {
                continue;
            }

            float pulseContribution = getPulseContribution(distance);
            float sanContribution = getSanContribution(distance);

            Faction faction = gameWorld.getRole(other) == null ? null : gameWorld.getRole(other).getFaction();
            if (faction == Faction.CIVILIAN) {
                calmPulse += pulseContribution;
                calmSan += sanContribution;
            } else {
                dangerPulse += pulseContribution;
                dangerSan += sanContribution;
            }
        }

        return new CowardThreatSnapshot(dangerPulse, calmPulse, dangerSan, calmSan);
    }

    private static float getPulseContribution(double distance) {
        if (distance <= CowardConstants.BAND_1_RADIUS) {
            return CowardConstants.BAND_1_PULSE;
        }
        if (distance <= CowardConstants.BAND_2_RADIUS) {
            return CowardConstants.BAND_2_PULSE;
        }
        if (distance <= CowardConstants.BAND_3_RADIUS) {
            return CowardConstants.BAND_3_PULSE;
        }
        return CowardConstants.BAND_4_PULSE;
    }

    private static float getSanContribution(double distance) {
        if (distance <= CowardConstants.BAND_1_RADIUS) {
            return CowardConstants.BAND_1_SAN;
        }
        if (distance <= CowardConstants.BAND_2_RADIUS) {
            return CowardConstants.BAND_2_SAN;
        }
        if (distance <= CowardConstants.BAND_3_RADIUS) {
            return CowardConstants.BAND_3_SAN;
        }
        return CowardConstants.BAND_4_SAN;
    }

    public float netPulse() {
        return Math.max(0.0f, this.dangerPulse - this.calmPulse);
    }

    public boolean hasEffectiveDanger() {
        return this.netPulse() > 0.0f;
    }

    public float sanMultiplier() {
        return MathHelper.clamp(
                1.0f + this.dangerSan - this.calmSan,
                CowardConstants.MIN_SAN_MULTIPLIER,
                CowardConstants.MAX_SAN_MULTIPLIER
        );
    }

    public int pulseIntervalTicks() {
        float intervalSeconds = MathHelper.clamp(5.0f - 4.0f * this.netPulse(), 0.8f, 4.0f);
        return Math.max(CowardConstants.MIN_PULSE_INTERVAL_TICKS, Math.round(intervalSeconds * 20.0f));
    }

    public float revolverShakeStrength() {
        float strength = CowardConstants.REVOLVER_BASE_SHAKE
                + (this.dangerPulse - this.calmPulse) * CowardConstants.REVOLVER_SHAKE_SCALAR;
        return MathHelper.clamp(
                strength,
                CowardConstants.REVOLVER_MIN_SHAKE,
                CowardConstants.REVOLVER_MAX_SHAKE
        );
    }
}
