package org.agmas.noellesroles.client.roles.myers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.roles.myers.MyersConstants;
import org.agmas.noellesroles.roles.myers.MyersPsychoHandler;
import org.jetbrains.annotations.Nullable;

/** 迈尔斯 12 格范围疯魔音乐，按本地玩家与迈尔斯距离独立淡入淡出。 */
public final class MyersMusicController {
    private static @Nullable LoopSoundInstance loop;
    private static @Nullable PlayerEntity source;

    private MyersMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            reset();
            return;
        }

        PlayerEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (PlayerEntity player : client.world.getPlayers()) {
            if (!MyersPsychoHandler.isActive(player)) continue;
            double distance = client.player.distanceTo(player);
            if (distance <= MyersConstants.EVIL_POSSESS_MUSIC_RADIUS && distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        if (nearest != null) {
            if (loop == null || loop.isDone() || source != nearest) {
                if (loop != null) loop.stopNow();
                source = nearest;
                loop = new LoopSoundInstance();
                client.getSoundManager().play(loop);
            }
            loop.updateSource(nearest, nearestDistance);
            loop.resumeFromFadeOut();
        } else if (loop != null) {
            loop.requestFadeOut();
        }

        if (loop != null && loop.isDone()) {
            loop = null;
            source = null;
        }
    }

    public static void reset() {
        if (loop != null) loop.stopNow();
        loop = null;
        source = null;
    }

    private static final class LoopSoundInstance extends MovingSoundInstance {
        private boolean fadingOut;
        private int fadeTicks;
        private int fadeOutTicks;
        private float fadeOutStartVolume;

        private LoopSoundInstance() {
            super(NoellesRolesSounds.AMBIENT_MYERS, SoundCategory.MASTER, Random.create());
            repeat = true;
            repeatDelay = 0;
            relative = false;
            attenuationType = SoundInstance.AttenuationType.LINEAR;
            volume = MyersConstants.MUSIC_MIN_START_VOLUME;
        }

        private void updateSource(PlayerEntity player, double distance) {
            x = player.getX();
            y = player.getY();
            z = player.getZ();
            float distanceFactor = (float) Math.max(0.0D, 1.0D - distance / MyersConstants.EVIL_POSSESS_MUSIC_RADIUS);
            volume = Math.max(MyersConstants.MUSIC_MIN_START_VOLUME, MyersConstants.EVIL_POSSESS_MUSIC_VOLUME * distanceFactor * fadeProgress());
        }

        @Override
        public void tick() {
            if (fadingOut) {
                fadeOutTicks++;
                float progress = 1.0F - fadeOutTicks / (float) MyersConstants.EVIL_POSSESS_MUSIC_FADE_OUT_TICKS;
                volume = Math.max(0.0F, fadeOutStartVolume * progress);
                if (fadeOutTicks >= MyersConstants.EVIL_POSSESS_MUSIC_FADE_OUT_TICKS) setDone();
            } else if (fadeTicks < MyersConstants.EVIL_POSSESS_MUSIC_FADE_IN_TICKS) {
                fadeTicks++;
            }
        }

        private float fadeProgress() {
            return Math.min(1.0F, fadeTicks / (float) MyersConstants.EVIL_POSSESS_MUSIC_FADE_IN_TICKS);
        }

        private void requestFadeOut() {
            if (!fadingOut) {
                fadingOut = true;
                fadeOutTicks = 0;
                fadeOutStartVolume = volume;
            }
        }

        private void resumeFromFadeOut() {
            fadingOut = false;
            fadeOutTicks = 0;
        }

        private void stopNow() { setDone(); }
    }
}
