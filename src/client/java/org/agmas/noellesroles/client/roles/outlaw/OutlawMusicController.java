package org.agmas.noellesroles.client.roles.outlaw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.roles.outlaw.OutlawWorldComponent;
import org.jetbrains.annotations.Nullable;

/** 亡命时刻全场循环音乐控制器。 */
public final class OutlawMusicController {
    private static @Nullable LoopSoundInstance loop;

    private OutlawMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null) {
            reset(client);
            return;
        }
        boolean shouldPlay = OutlawWorldComponent.KEY.get(client.world).hasActiveOutlaw();
        if (shouldPlay) {
            if (loop == null || loop.isDone() || !client.getSoundManager().isPlaying(loop)) {
                loop = new LoopSoundInstance();
                client.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.stopNow();
            loop = null;
        }
    }

    public static void reset(MinecraftClient client) {
        if (loop != null) {
            loop.stopNow();
            loop = null;
        }
    }

    private static final class LoopSoundInstance extends MovingSoundInstance {
        private LoopSoundInstance() {
            super(NoellesRolesSounds.AMBIENT_RESURRECTED_OUTLAW, SoundCategory.MASTER, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.volume = 0.9F;
            this.pitch = 1.0F;
        }

        @Override
        public void tick() {
            // 循环音乐只需要由 SoundManager 按 repeat 播放，不需要额外的每 tick 状态机。
        }

        private void stopNow() {
            setDone();
        }
    }
}
