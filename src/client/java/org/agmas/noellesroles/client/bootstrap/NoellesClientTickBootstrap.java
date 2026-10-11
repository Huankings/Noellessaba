package org.agmas.noellesroles.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.agmas.noellesroles.client.appearance.NoellesAppearanceClientState;
import org.agmas.noellesroles.client.appearance.roles.insane_observer.InsaneObserverClientState;
import org.agmas.noellesroles.client.input.NoellesAbilityInputHandler;
import org.agmas.noellesroles.client.items.NoellesRolesGrenadeInputHandler;
import org.agmas.noellesroles.client.roles.coward.CowardClientEffects;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityClientEffects;
import org.agmas.noellesroles.client.roles.jason.JasonAbilityClientSoundController;
import org.agmas.noellesroles.client.roles.licensed_villain.LicensedVillainMusicController;
import org.agmas.noellesroles.client.roles.myers.MyersAbilityInputHandler;
import org.agmas.noellesroles.client.roles.myers.MyersMusicController;
import org.agmas.noellesroles.client.roles.outlaw.OutlawMusicController;
import org.agmas.noellesroles.client.roles.perceiver.PerceiverClientPerceptionReporter;
import org.agmas.noellesroles.client.roles.rememberer.RemembererClientEffects;
import org.agmas.noellesroles.client.roles.shadow_jester.ShadowJesterMusicController;
import org.agmas.noellesroles.client.roles.spiritualist.SpiritualistClientController;
import org.agmas.noellesroles.client.roles.stalker.StalkerAbilityInputHandler;
import org.agmas.noellesroles.client.roles.vulture.VultureAbilityInputHandler;
import org.agmas.noellesroles.client.roles.timekeeper.TimekeeperWatchInputHandler;

/** 客户端每帧调度器，只聚合各职责 provider。 */
public final class NoellesClientTickBootstrap {
    private NoellesClientTickBootstrap() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(NoellesClientTickBootstrap::tick);
    }

    private static void tick(MinecraftClient client) {
        PerceiverClientPerceptionReporter.tick(client);
        RemembererClientEffects.tick(client);
        CowardClientEffects.tick(client);
        JasonAbilityClientEffects.tick(client);
        ShadowJesterMusicController.tick(client);
        LicensedVillainMusicController.tick(client);
        OutlawMusicController.tick(client);
        MyersMusicController.tick(client);
        JasonAbilityClientSoundController.tick(client);
        MyersAbilityInputHandler.tick(client);
        StalkerAbilityInputHandler.tick(client);
        VultureAbilityInputHandler.tick(client);
        NoellesAbilityInputHandler.tick(client);
        NoellesRolesGrenadeInputHandler.tick(client);
        NoellesAppearanceClientState.refresh(client);
        InsaneObserverClientState.tick();
        NoellesRolesGrenadeInputHandler.releaseIfAttackNotHeld(client.options.attackKey.isPressed());
        TimekeeperWatchInputHandler.releaseIfAttackNotHeld(client.options.attackKey.isPressed());
    }
}
