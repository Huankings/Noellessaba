package org.agmas.noellesroles.client.roles.convener;

import java.util.UUID;

/**
 * 召集者专用流动配色。
 */
public final class ConvenerColorHelper {
    private static final long FLOW_PERIOD_MS = 12_000L;

    public static final int[] FLOW_COLORS = new int[] {
            0x95D6FF,
            0x54C1FF,
            0x1B6BFF,
            0xB29BFF,
            0x7B5CFF,
            0x4E2ED6,
            0xFFB0E4,
            0xFF78C9,
            0xE63B9E
    };

    private ConvenerColorHelper() {
    }

    public static int getBarFlowColor(int x, int width, float alpha) {
        float position = ((float) x / Math.max(1, width - 1)) * FLOW_COLORS.length;
        return getAnimatedColor(getCurrentPhase() + position, alpha);
    }

    public static int getPlayerFlowColor(UUID uuid) {
        int hash = Math.floorMod(uuid.hashCode(), 4096);
        float uuidOffset = ((float) hash / 4096.0F) * FLOW_COLORS.length;
        return getAnimatedColor(getCurrentPhase() + uuidOffset, 1.0F);
    }

    public static int getAnimatedColor(float palettePosition, float alpha) {
        float floor = (float) Math.floor(palettePosition);
        int leftIndex = Math.floorMod((int) floor, FLOW_COLORS.length);
        int rightIndex = (leftIndex + 1) % FLOW_COLORS.length;
        float blend = palettePosition - floor;
        return interpolateColor(FLOW_COLORS[leftIndex], FLOW_COLORS[rightIndex], blend, alpha);
    }

    private static float getCurrentPhase() {
        long current = System.currentTimeMillis() % FLOW_PERIOD_MS;
        return ((float) current / (float) FLOW_PERIOD_MS) * FLOW_COLORS.length;
    }

    private static int interpolateColor(int left, int right, float progress, float alpha) {
        int lr = (left >> 16) & 0xFF;
        int lg = (left >> 8) & 0xFF;
        int lb = left & 0xFF;
        int rr = (right >> 16) & 0xFF;
        int rg = (right >> 8) & 0xFF;
        int rb = right & 0xFF;

        int r = Math.round(lr + (rr - lr) * progress);
        int g = Math.round(lg + (rg - lg) * progress);
        int b = Math.round(lb + (rb - lb) * progress);
        int a = Math.round(alpha * 255.0F) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
