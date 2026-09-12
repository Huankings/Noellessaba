package org.agmas.noellesroles.client.roles.licensed_villain;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainConstants;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainMomentWorldComponent;
import org.agmas.noellesroles.roles.outlaw.OutlawWorldComponent;
import org.jetbrains.annotations.Nullable;

/** 执照恶棍时刻的全场循环环境音控制器。 */
public final class LicensedVillainMusicController {
    private static @Nullable LoopSoundInstance loopInstance;

    private LicensedVillainMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            reset(client);
            return;
        }

        if (OutlawWorldComponent.KEY.get(client.world).hasActiveOutlaw()) {
            /* 亡命时刻要求立即让位，因此这里不走淡出，确保同一 tick 内停止旧音乐。 */
            stopLoopImmediately();
            return;
        }

        if (LicensedVillainMomentWorldComponent.KEY.get(client.world).hasActiveMoment()) {
            startOrResumeLoop(client);
        } else {
            /* 执照恶棍死亡或普通状态失效时按需求平滑淡出。 */
            fadeOutLoop();
        }

        if (loopInstance != null && loopInstance.isDone()) {
            loopInstance = null;
        }
    }

    public static void reset(MinecraftClient client) {
        stopLoopImmediately();
    }

    private static void startOrResumeLoop(MinecraftClient client) {
        if (loopInstance != null && !loopInstance.isDone() && client.getSoundManager().isPlaying(loopInstance)) {
            loopInstance.resumeFromFadeOut();
            return;
        }
        loopInstance = new LoopSoundInstance();
        client.getSoundManager().play(loopInstance);
    }

    private static void fadeOutLoop() {
        if (loopInstance != null && !loopInstance.isDone()) {
            loopInstance.requestFadeOut();
        }
    }

    private static void stopLoopImmediately() {
        if (loopInstance == null) {
            return;
        }
        loopInstance.stopNow();
        loopInstance = null;
    }

    private static final class LoopSoundInstance extends MovingSoundInstance {
        private boolean fadingOut;
        private float fadeOutStartVolume;

        private LoopSoundInstance() {
            super(NoellesRolesSounds.AMBIENT_LICENSED_VILLAIN, SoundCategory.MASTER, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.volume = Math.min(0.02F, LicensedVillainConstants.MUSIC_BASE_VOLUME);
            this.pitch = LicensedVillainConstants.MUSIC_PITCH;
        }

        @Override
        public void tick() {
            if (this.fadingOut) {
                this.volume = Math.max(0.0F, this.volume - LicensedVillainConstants.MUSIC_FADE_STEP);
                if (this.volume <= 0.0F) {
                    setDone();
                }
                return;
            }
            this.volume = Math.min(
                    LicensedVillainConstants.MUSIC_BASE_VOLUME,
                    this.volume + LicensedVillainConstants.MUSIC_FADE_STEP
            );
        }

        private void requestFadeOut() {
            if (!this.fadingOut) {
                this.fadingOut = true;
                this.fadeOutStartVolume = this.volume;
            }
        }

        private void resumeFromFadeOut() {
            if (!this.fadingOut) {
                return;
            }
            this.fadingOut = false;
            this.volume = Math.max(this.volume, Math.min(0.02F, this.fadeOutStartVolume));
        }

        private void stopNow() {
            setDone();
        }
    }
}
