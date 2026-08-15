package org.agmas.noellesroles.client.roles.jason;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.roles.jason.JasonAbilityPlayerComponent;
import org.agmas.noellesroles.roles.jason.JasonConstants;
import org.jetbrains.annotations.Nullable;

/**
 * 杰森无恶不在的客户端音效控制器。
 */
public final class JasonAbilityClientSoundController {
    private static @Nullable LoopSoundInstance loopInstance;
    private static boolean loopSuppressedByServerStop;

    private JasonAbilityClientSoundController() {
    }

    public static void handle(MinecraftClient client, org.agmas.noellesroles.packet.role.jason.JasonAbilitySoundS2CPacket.Action action) {
        if (client == null) {
            return;
        }

        switch (action) {
            case PLAY_START -> playOneShot(client, NoellesRolesSounds.AMBIENT_JASON_ABILITY);
            case START_LOOP -> startLoop(client);
            case STOP_LOOP -> {
                /*
                 * 服务端主动要求停持续音时，客户端组件同步可能还停留在 ACTIVE 一两帧。
                 * 这里先设置抑制标记，避免本地 tick 根据旧组件状态马上把循环音又拉起来。
                 * 真正的声音不再硬停，而是按退出过渡时间淡出，避免“持续音突然被掐断”的割裂感。
                 */
                loopSuppressedByServerStop = true;
                fadeOutLoop();
            }
            case PLAY_END -> playOneShot(client, NoellesRolesSounds.AMBIENT_JASON_ABILITY_END);
            case PLAY_JUMP_SCARE -> playOneShot(client, NoellesRolesSounds.AMBIENT_JASON_JUMP_SCARE);
        }
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null || client.player == null) {
            stopLoopImmediately();
            loopSuppressedByServerStop = false;
            return;
        }

        boolean hasActiveLikeAbility = false;
        boolean shouldPlayLoop = false;
        for (PlayerEntity player : client.world.getPlayers()) {
            JasonAbilityPlayerComponent component = JasonAbilityPlayerComponent.KEY.get(player);
            if (component.isActiveLike()) {
                hasActiveLikeAbility = true;
            }
            if (component.isFullyActive()) {
                shouldPlayLoop = true;
            }
        }

        if (!hasActiveLikeAbility) {
            /*
             * 只有所有无恶不在阶段都结束后才解除 STOP_LOOP 抑制。
             * 这样下一次重新发动仍可正常播放，当前这次主动解除不会被旧同步状态误重启。
             */
            loopSuppressedByServerStop = false;
        }

        if (shouldPlayLoop && !loopSuppressedByServerStop) {
            startLoop(client);
        } else {
            fadeOutLoop();
        }

        if (loopInstance != null && loopInstance.isDone()) {
            loopInstance = null;
        }
    }

    public static void reset(MinecraftClient client) {
        loopSuppressedByServerStop = false;
        stopLoopImmediately();
    }

    private static void startLoop(MinecraftClient client) {
        loopSuppressedByServerStop = false;
        if (loopInstance != null && !loopInstance.isDone() && client.getSoundManager().isPlaying(loopInstance)) {
            loopInstance.resumeFromFadeOut();
            return;
        }
        LoopSoundInstance instance = new LoopSoundInstance();
        loopInstance = instance;
        client.getSoundManager().play(instance);
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

    private static void playOneShot(MinecraftClient client, net.minecraft.sound.SoundEvent soundEvent) {
        client.getSoundManager().play(PositionedSoundInstance.master(
                soundEvent,
                JasonConstants.ABILITY_SOUND_PITCH,
                JasonConstants.ABILITY_SOUND_VOLUME
        ));
    }

    private static final class LoopSoundInstance extends MovingSoundInstance {
        private int fadeTicks;
        private int fadeOutTicks;
        private float fadeOutStartVolume;
        private boolean fadingOut;

        private LoopSoundInstance() {
            super(NoellesRolesSounds.AMBIENT_JASON_ABILITY_LAST, SoundCategory.MASTER, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            /*
             * SoundManager 在部分情况下会跳过初始音量为 0 的声音实例。
             * 这里用一个极低但非 0 的起始音量保证实例真实进入播放队列，随后 tick 中继续按常量淡入。
             */
            this.volume = Math.min(0.02F, JasonConstants.ABILITY_SOUND_VOLUME);
            this.pitch = JasonConstants.ABILITY_SOUND_PITCH;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
        }

        @Override
        public void tick() {
            if (this.fadingOut) {
                /*
                 * 退出无恶不在时，持续音按显形过渡同步淡出。
                 * 这样主动退出、死亡被动清理和杰森模式强制退出都能复用同一条声音收尾路径。
                 */
                this.fadeOutTicks++;
                int fadeOutDuration = Math.max(1, JasonConstants.ABILITY_LOOP_SOUND_FADE_OUT_TICKS);
                float remainingProgress = Math.max(0.0F, 1.0F - this.fadeOutTicks / (float) fadeOutDuration);
                this.volume = this.fadeOutStartVolume * remainingProgress;
                if (this.fadeOutTicks >= fadeOutDuration) {
                    this.setDone();
                }
                return;
            }

            if (this.fadeTicks < JasonConstants.ABILITY_LOOP_SOUND_FADE_IN_TICKS) {
                this.fadeTicks++;
                this.volume = Math.min(
                        JasonConstants.ABILITY_SOUND_VOLUME,
                        JasonConstants.ABILITY_SOUND_VOLUME * (this.fadeTicks / (float) JasonConstants.ABILITY_LOOP_SOUND_FADE_IN_TICKS)
                );
            } else {
                this.volume = JasonConstants.ABILITY_SOUND_VOLUME;
            }
        }

        private void requestFadeOut() {
            if (this.fadingOut) {
                return;
            }
            this.fadingOut = true;
            this.fadeOutTicks = 0;
            this.fadeOutStartVolume = this.volume;
        }

        private void resumeFromFadeOut() {
            if (!this.fadingOut) {
                return;
            }
            /*
             * 如果服务端因为重同步再次确认持续音应播放，撤销淡出并从当前淡入进度继续，
             * 防止客户端短暂误判导致循环音被永久压没。
             */
            this.fadingOut = false;
            this.fadeOutTicks = 0;
            this.fadeOutStartVolume = 0.0F;
        }

        private void stopNow() {
            this.setDone();
        }
    }
}
