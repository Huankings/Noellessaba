package org.agmas.noellesroles.client.roles.killer_queen;

import dev.doctor4t.wathe.api.psycho.PsychoModeApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;
import org.agmas.noellesroles.NoellesRolesSounds;
import org.agmas.noellesroles.roles.killer_queen.KillerQueenPsychoHandler;
import org.agmas.noellesroles.roles.licensed_villain.LicensedVillainMomentWorldComponent;
import org.agmas.noellesroles.roles.outlaw.OutlawWorldComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterComponent;
import org.agmas.noellesroles.roles.shadow_jester.ShadowJesterMusicTheme;
import org.jetbrains.annotations.Nullable;

/** 杀手皇后疯魔音乐控制器；机制不停，只在更高优先级特殊时刻期间静音。 */
public final class KillerQueenMusicController {
    private static @Nullable LoopSound loop;

    private KillerQueenMusicController() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.world == null) {
            reset(client);
            return;
        }
        boolean higherPriority = OutlawWorldComponent.KEY.get(client.world).hasActiveOutlaw()
                || LicensedVillainMomentWorldComponent.KEY.get(client.world).hasActiveMoment()
                || hasActiveShadowJesterMoment(client);
        boolean activeQueen = client.world.getPlayers().stream()
                .anyMatch(player -> PsychoModeApi.isActive(player, KillerQueenPsychoHandler.PROFILE_ID));
        if (activeQueen && !higherPriority) {
            if (loop == null || loop.isDone() || !client.getSoundManager().isPlaying(loop)) {
                loop = new LoopSound();
                client.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.stopNow();
            loop = null;
        }
    }

    private static boolean hasActiveShadowJesterMoment(MinecraftClient client) {
        ShadowJesterComponent state = ShadowJesterComponent.KEY.get(client.world);
        return state.getPhaseFourTheme() != ShadowJesterMusicTheme.NONE
                && !state.isPhaseFourSuspended()
                && !state.areBothPairMembersConfirmedOrPendingDeath();
    }

    public static void reset(MinecraftClient client) {
        if (loop != null) loop.stopNow();
        loop = null;
    }

    private static final class LoopSound extends MovingSoundInstance {
        private LoopSound() {
            super(NoellesRolesSounds.AMBIENT_KILLER_QUEEN, SoundCategory.MASTER, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.volume = org.agmas.noellesroles.roles.killer_queen.KillerQueenConstants.PSYCHO_MUSIC_VOLUME;
            this.pitch = org.agmas.noellesroles.roles.killer_queen.KillerQueenConstants.PSYCHO_MUSIC_PITCH;
        }

        @Override public void tick() { }
        private void stopNow() { setDone(); }
    }
}
