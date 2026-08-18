package org.agmas.noellesroles.client.roles.shadow_jester;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterConstants;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterMusicTheme;
import org.jetbrains.annotations.Nullable;

/**
 * 影子小丑谢幕音乐的客户端循环控制。
 */
public final class ShadowJesterMusicController {
    private static @Nullable LoopSoundInstance loopInstance;
    private static ShadowJesterMusicTheme activeTheme = ShadowJesterMusicTheme.NONE;

    private ShadowJesterMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            reset(client);
            return;
        }

        ShadowJesterComponent component = ShadowJesterComponent.KEY.get(client.world);
        ShadowJesterMusicTheme desiredTheme = resolveDesiredTheme(component);
        if (desiredTheme == ShadowJesterMusicTheme.NONE) {
            fadeOutLoop();
        } else {
            startLoop(client, desiredTheme);
        }

        if (loopInstance != null && loopInstance.isDone()) {
            loopInstance = null;
            activeTheme = ShadowJesterMusicTheme.NONE;
        }
    }

    public static void reset(MinecraftClient client) {
        stopLoopImmediately();
        activeTheme = ShadowJesterMusicTheme.NONE;
    }

    private static ShadowJesterMusicTheme resolveDesiredTheme(ShadowJesterComponent component) {
        ShadowJesterMusicTheme theme = component.getPhaseFourTheme();
        if (component.areBothPairMembersConfirmedOrPendingDeath()) {
            return ShadowJesterMusicTheme.NONE;
        }
        /*
         * 第四阶段谢幕音乐是整场对局的全服氛围音，而不是影子小丑本人的私有提示。
         * 服务端只把“当前是否处于第四阶段、应该播放 King 还是 Queen”写进世界组件；
         * 客户端拿到非 NONE 主题后，无论本地玩家是什么职业、是否存活、是否旁观，都应开始循环播放。
         * 只有双方都已经有明确死亡事实时才淡出；普通 creative / spectator 调试不算死亡，
         * 避免测试时因为本地玩家临时不在 alivePlayers 里就提前停掉全服音乐。
         */
        return theme;
    }

    private static void startLoop(MinecraftClient client, ShadowJesterMusicTheme theme) {
        /*
         * 主题保存在世界组件里，会随时停者回溯一起恢复。
         * 因此客户端不记“是否已经播放过”，而是每 tick 以组件状态为准；
         * 回溯后如果仍处于第四阶段，这里会自动重新拉起正确主题。
         */
        if (loopInstance != null && !loopInstance.isDone() && client.getSoundManager().isPlaying(loopInstance)) {
            if (activeTheme == theme) {
                loopInstance.resumeFromFadeOut();
                return;
            }
            /*
             * 主题极少会在正常时间线切换，主要来自回溯恢复。
             * 此时直接结束旧主题再启动新主题，避免两段谢幕音乐重叠。
             */
            loopInstance.stopNow();
            loopInstance = null;
        }

        LoopSoundInstance instance = new LoopSoundInstance(soundForTheme(theme));
        loopInstance = instance;
        activeTheme = theme;
        client.getSoundManager().play(instance);
    }

    private static SoundEvent soundForTheme(ShadowJesterMusicTheme theme) {
        return theme == ShadowJesterMusicTheme.KING
                ? NoellesRolesSounds.AMBIENT_SHADOW_JESTER_KING
                : NoellesRolesSounds.AMBIENT_SHADOW_JESTER_QUEEN;
    }

    private static void fadeOutLoop() {
        if (loopInstance == null || loopInstance.isDone()) {
            return;
        }
        loopInstance.requestFadeOut();
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

        private LoopSoundInstance(SoundEvent soundEvent) {
            super(soundEvent, SoundCategory.MASTER, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.volume = Math.min(0.02F, ShadowJesterConstants.MUSIC_BASE_VOLUME);
            this.pitch = ShadowJesterConstants.MUSIC_PITCH;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        }

        @Override
        public void tick() {
            if (this.fadingOut) {
                this.volume = Math.max(0.0F, this.volume - ShadowJesterConstants.MUSIC_FADE_STEP);
                if (this.volume <= 0.0F) {
                    this.setDone();
                }
                return;
            }
            this.volume = Math.min(
                    ShadowJesterConstants.MUSIC_BASE_VOLUME,
                    this.volume + ShadowJesterConstants.MUSIC_FADE_STEP
            );
        }

        private void requestFadeOut() {
            if (this.fadingOut) {
                return;
            }
            this.fadingOut = true;
            this.fadeOutStartVolume = this.volume;
        }

        private void resumeFromFadeOut() {
            if (!this.fadingOut) {
                return;
            }
            /*
             * 客户端组件同步偶尔会短暂回退一帧；如果下一帧又确认第四阶段仍有效，
             * 取消淡出并从当前音量继续淡入，避免音乐被一次误判永久压没。
             */
            this.fadingOut = false;
            this.volume = Math.max(this.volume, Math.min(0.02F, this.fadeOutStartVolume));
        }

        private void stopNow() {
            this.setDone();
        }
    }
}
